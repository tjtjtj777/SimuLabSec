param(
    [string]$BaseUrl = "http://localhost:8080/simulab",
    [string]$Username = "demo",
    [string]$Password = "demo123456",
    [int]$TimeoutSec = 8
)

$ErrorActionPreference = "Stop"

# 统一收集每个检查项的结果，脚本末尾输出汇总并返回明确退出码。
$results = New-Object System.Collections.Generic.List[object]

function Add-Result {
    param(
        [string]$Name,
        [bool]$Passed,
        [string]$Detail
    )
    $status = if ($Passed) { "PASS" } else { "FAIL" }
    $results.Add([pscustomobject]@{
            name   = $Name
            status = $status
            detail = $Detail
        })
}

function Invoke-JsonRequest {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Url,
        [hashtable]$Headers,
        [string]$Body
    )
    if ($Body) {
        return Invoke-WebRequest -Method $Method -Uri $Url -Headers $Headers -Body $Body -ContentType "application/json" -TimeoutSec $TimeoutSec -SkipHttpErrorCheck
    }
    return Invoke-WebRequest -Method $Method -Uri $Url -Headers $Headers -TimeoutSec $TimeoutSec -SkipHttpErrorCheck
}

Write-Host "== SimuLab Backend Smoke Test ==" -ForegroundColor Cyan
Write-Host "BaseUrl: $BaseUrl"
Write-Host "TimeoutSec: $TimeoutSec"

# 1) 登录 demo 账号并提取 JWT。
$token = $null
try {
    $loginBody = @{ username = $Username; password = $Password } | ConvertTo-Json
    $loginResp = Invoke-JsonRequest -Method "POST" -Url "$BaseUrl/api/auth/login" -Body $loginBody
    if ($loginResp.StatusCode -eq 200) {
        $loginJson = $loginResp.Content | ConvertFrom-Json
        $token = $loginJson.data.accessToken
        if ([string]::IsNullOrWhiteSpace($token)) {
            Add-Result -Name "auth.login" -Passed $false -Detail "HTTP 200 但 accessToken 为空"
        }
        else {
            Add-Result -Name "auth.login" -Passed $true -Detail "HTTP 200, token 长度 $($token.Length)"
        }
    }
    else {
        Add-Result -Name "auth.login" -Passed $false -Detail "HTTP $($loginResp.StatusCode), body=$($loginResp.Content)"
    }
}
catch {
    Add-Result -Name "auth.login" -Passed $false -Detail $_.Exception.Message
}

# 2) 使用 JWT 调用 lots / layers / wafers 最小链路。
$authHeaders = @{}
if (-not [string]::IsNullOrWhiteSpace($token)) {
    $authHeaders["Authorization"] = "Bearer $token"
}

$checks = @(
    @{ name = "lot.page";   path = "/api/lots?pageNo=1&pageSize=5" },
    @{ name = "layer.page"; path = "/api/layers?pageNo=1&pageSize=5" },
    @{ name = "wafer.page"; path = "/api/wafers?pageNo=1&pageSize=5" }
)

foreach ($check in $checks) {
    try {
        $resp = Invoke-JsonRequest -Method "GET" -Url "$BaseUrl$($check.path)" -Headers $authHeaders
        if ($resp.StatusCode -eq 200) {
            $json = $resp.Content | ConvertFrom-Json
            Add-Result -Name $check.name -Passed $true -Detail "HTTP 200, success=$($json.success)"
        }
        else {
            Add-Result -Name $check.name -Passed $false -Detail "HTTP $($resp.StatusCode), body=$($resp.Content)"
        }
    }
    catch {
        Add-Result -Name $check.name -Passed $false -Detail $_.Exception.Message
    }
}

Write-Host ""
Write-Host "== 结果汇总 ==" -ForegroundColor Cyan
$results | ForEach-Object {
    $color = if ($_.status -eq "PASS") { "Green" } else { "Red" }
    Write-Host ("[{0}] {1} - {2}" -f $_.status, $_.name, $_.detail) -ForegroundColor $color
}

$failed = ($results | Where-Object { $_.status -eq "FAIL" }).Count
if ($failed -gt 0) {
    Write-Host ""
    Write-Host "Smoke test 未通过，失败项数量: $failed" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Smoke test 通过。" -ForegroundColor Green
exit 0
