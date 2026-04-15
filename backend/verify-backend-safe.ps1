param(
    [int]$Port = 18080,
    [int]$StartupTimeoutSec = 90,
    [int]$SmokeRequestTimeoutSec = 8,
    [int]$SmokeScriptTimeoutSec = 60,
    [switch]$SkipCompile
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$pidFile = Join-Path $PSScriptRoot "verify-app.pid"
$outLog = Join-Path $PSScriptRoot "verify-app.out.log"
$errLog = Join-Path $PSScriptRoot "verify-app.err.log"

function Stop-IfRunning {
    param([int]$ProcessId)
    if ($ProcessId -le 0) { return }
    $p = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
    if ($null -ne $p) {
        Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
    }
}

function Remove-StalePid {
    param([string]$PidPath)
    if (-not (Test-Path $PidPath)) { return }
    $rawPid = (Get-Content -Raw $PidPath).Trim()
    $oldPid = 0
    if ([int]::TryParse($rawPid, [ref]$oldPid)) {
        Stop-IfRunning -ProcessId $oldPid
    }
    Remove-Item -LiteralPath $PidPath -Force -ErrorAction SilentlyContinue
}

function Wait-BackendReady {
    param(
        [int]$ProcessId,
        [string]$StdOutPath,
        [string]$StdErrPath,
        [int]$TimeoutSec
    )
    $start = Get-Date
    while ($true) {
        $proc = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
        if ($null -eq $proc) {
            return @{ Ready = $false; Reason = "backend process exited unexpectedly" }
        }

        $elapsed = ((Get-Date) - $start).TotalSeconds
        if ($elapsed -ge $TimeoutSec) {
            return @{ Ready = $false; Reason = "startup timeout after ${TimeoutSec}s" }
        }

        $outText = if (Test-Path $StdOutPath) { Get-Content -Raw $StdOutPath } else { "" }
        $errText = if (Test-Path $StdErrPath) { Get-Content -Raw $StdErrPath } else { "" }
        if ($outText -match "Started .*Application") {
            return @{ Ready = $true; Reason = "startup success marker detected" }
        }
        if (($outText -match "APPLICATION FAILED TO START") -or ($errText -match "APPLICATION FAILED TO START")) {
            return @{ Ready = $false; Reason = "application failed to start" }
        }

        Start-Sleep -Milliseconds 500
    }
}

Remove-StalePid -PidPath $pidFile
Remove-Item -LiteralPath $outLog -Force -ErrorAction SilentlyContinue
Remove-Item -LiteralPath $errLog -Force -ErrorAction SilentlyContinue

$listener = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -eq $Port } | Select-Object -First 1
if ($null -ne $listener) {
    throw "port $Port is already in use by pid $($listener.OwningProcess)"
}

if (-not $SkipCompile) {
    mvn "-Dmaven.repo.local=.m2repo-run" -DskipTests compile | Out-Host
    if ($LASTEXITCODE -ne 0) {
        throw "maven compile failed"
    }
}

if (Test-Path "$PSScriptRoot\java.cp.txt") {
    $cp = (Get-Content "$PSScriptRoot\java.cp.txt" -Raw).Trim()
}
elseif (Test-Path "$PSScriptRoot\java.cp.deps.txt") {
    $deps = (Get-Content "$PSScriptRoot\java.cp.deps.txt" -Raw).Trim()
    $cp = "target\classes;$deps"
}
else {
    throw "No classpath file found. Please generate java.cp.txt first."
}

$backendPid = 0
$exitCode = 0

try {
    $app = Start-Process -FilePath "java" `
        -ArgumentList @("-Dserver.port=$Port", "-cp", $cp, "com.simulab.SimuLabBackendApplication") `
        -WorkingDirectory $PSScriptRoot `
        -RedirectStandardOutput $outLog `
        -RedirectStandardError $errLog `
        -PassThru

    $backendPid = $app.Id
    Set-Content -LiteralPath $pidFile -Value $backendPid -Encoding ascii
    Write-Host "backend started, pid=$backendPid, port=$Port"

    $ready = Wait-BackendReady -ProcessId $backendPid -StdOutPath $outLog -StdErrPath $errLog -TimeoutSec $StartupTimeoutSec
    if (-not $ready.Ready) {
        throw "backend startup failed: $($ready.Reason)"
    }
    Write-Host "backend startup check passed"

    $smoke = Start-Process -FilePath "pwsh" `
        -ArgumentList @("-NoProfile", "-File", "$PSScriptRoot\smoke-test.ps1", "-BaseUrl", "http://localhost:$Port/simulab", "-TimeoutSec", "$SmokeRequestTimeoutSec") `
        -WorkingDirectory $PSScriptRoot `
        -NoNewWindow `
        -PassThru

    if (-not $smoke.WaitForExit($SmokeScriptTimeoutSec * 1000)) {
        Stop-Process -Id $smoke.Id -Force -ErrorAction SilentlyContinue
        throw "smoke-test timeout after ${SmokeScriptTimeoutSec}s"
    }
    if ($smoke.ExitCode -ne 0) {
        throw "smoke-test failed with exit code $($smoke.ExitCode)"
    }
    Write-Host "smoke test passed"
}
catch {
    $exitCode = 1
    Write-Host "verify failed: $($_.Exception.Message)" -ForegroundColor Red
    if (Test-Path $outLog) {
        Write-Host "--- verify-app.out.log (tail 80) ---" -ForegroundColor Yellow
        Get-Content -Tail 80 $outLog | Out-Host
    }
    if (Test-Path $errLog) {
        Write-Host "--- verify-app.err.log (tail 80) ---" -ForegroundColor Yellow
        Get-Content -Tail 80 $errLog | Out-Host
    }
}
finally {
    Stop-IfRunning -ProcessId $backendPid
    Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
}

exit $exitCode
