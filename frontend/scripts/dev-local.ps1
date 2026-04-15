$ErrorActionPreference = 'Stop'

$port = 5173

# 清理占用目标端口的旧 Vite 进程，避免重复执行 npm run dev 时端口冲突。
$connections = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue
foreach ($connection in $connections) {
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $($connection.OwningProcess)" -ErrorAction SilentlyContinue
    if ($null -ne $process -and $process.CommandLine -match 'vite') {
        Stop-Process -Id $connection.OwningProcess -Force -ErrorAction SilentlyContinue
        Start-Sleep -Milliseconds 500
    }
}

Write-Host "Starting Vite on http://127.0.0.1:$port ..."
$env:VITE_DEV_HOST = '127.0.0.1'
$env:VITE_DEV_PORT = "$port"
npx vite --host $env:VITE_DEV_HOST --port $env:VITE_DEV_PORT
