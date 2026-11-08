# 本地联调：并行启动候诊大屏 / 门边屏 / 签到终端 dev 服务（各开独立窗口）
# 前提：pdjh-server 已在 8081 运行；Vite 代理 /api -> 8081

param(
    [switch]$NoNewWindow
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot

$terminals = @(
    @{ Name = '候诊区大屏'; Dir = 'HJ候诊区大屏'; Port = 5173; Url = 'http://127.0.0.1:5173/screen/' }
    @{ Name = '自助签到'; Dir = 'HJ自助签到终端'; Port = 5174; Url = 'http://127.0.0.1:5174/checkin/' }
    @{ Name = '诊室门边屏'; Dir = 'HJ诊室门边屏终端'; Port = 5175; Url = 'http://127.0.0.1:5175/clinic/' }
)

Write-Host '>> 患者终端本地联调'
Write-Host "   后端 API: http://127.0.0.1:8081/api"
Write-Host ''

foreach ($t in $terminals) {
    $proj = Join-Path $Root $t.Dir
    if (-not (Test-Path (Join-Path $proj 'node_modules'))) {
        Write-Host "   安装依赖: $($t.Dir) ..."
        Push-Location $proj
        npm install
        Pop-Location
    }

    $cmd = "Set-Location '$proj'; npm run dev"
    if ($NoNewWindow) {
        Start-Job -Name $t.Name -ScriptBlock {
            param($Dir)
            Set-Location $Dir
            npm run dev 2>&1
        } -ArgumentList $proj | Out-Null
        Write-Host "   [Job] $($t.Name) -> $($t.Url)"
    }
    else {
        Start-Process powershell -ArgumentList @('-NoExit', '-Command', $cmd) | Out-Null
        Write-Host "   [窗口] $($t.Name) -> $($t.Url)"
    }
}

Write-Host ''
Write-Host '>> 启动后可在浏览器或安卓 Shell 指向上述 URL（dev 模式需本机可访问）'
Write-Host '>> API 自测: .\scripts\smoke-terminals.ps1 -ScreenDeviceCode <码> -ClinicDeviceCode <码>'

if ($NoNewWindow) {
    Write-Host '>> 后台 Job 运行中；Stop-Job * ; Remove-Job * 可停止'
}
