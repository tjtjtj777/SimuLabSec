$ErrorActionPreference = 'Stop'

Set-Location $PSScriptRoot

# 每次启动前先编译最新代码，避免运行旧的 target/classes。
mvn "-Dmaven.repo.local=.m2repo" -DskipTests compile | Out-Host
if ($LASTEXITCODE -ne 0) {
    throw "Maven compile failed, aborting startup."
}

# 日常启动优先复用已有 classpath 文件，避免每次启动都触发额外 Maven 下载。
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

java -cp $cp com.simulab.SimuLabBackendApplication
