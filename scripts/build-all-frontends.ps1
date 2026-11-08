# 全量前端构建：患者终端 + 管理后台

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot

& (Join-Path $PSScriptRoot 'build-terminals.ps1')

Write-Host '>> 构建 HJ管理后台/Gocent.Manager (/PDJHManagement/)...'
Push-Location (Join-Path $Root 'HJ管理后台\Gocent.Manager')
if (-not (Test-Path node_modules)) { npm install }
npm run build:prod
Pop-Location

Write-Host '>> 完成。产物：'
Write-Host '   HJ候诊区大屏/dist         -> Nginx /screen'
Write-Host '   HJ诊室门边屏终端/dist     -> Nginx /clinic'
Write-Host '   HJ自助签到终端/dist       -> Nginx /checkin'
Write-Host '   HJ管理后台/Gocent.Manager/dist   -> Nginx /PDJHManagement（或现场映射为 /admin）'
