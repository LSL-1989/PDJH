# 患者终端前端（候诊大屏 + 门边屏 + 自助签到）构建

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot

Write-Host '>> 构建 HJ候诊区大屏 (Vue3 /screen/)...'
Push-Location (Join-Path $Root 'HJ候诊区大屏')
if (-not (Test-Path node_modules)) { npm install }
npm run build
Pop-Location

Write-Host '>> 构建 HJ诊室门边屏终端 (/clinic/)...'
Push-Location (Join-Path $Root 'HJ诊室门边屏终端')
if (-not (Test-Path node_modules)) { npm install }
npm run build
Pop-Location

Write-Host '>> 构建 HJ自助签到终端 (/checkin/)...'
Push-Location (Join-Path $Root 'HJ自助签到终端')
if (-not (Test-Path node_modules)) { npm install }
npm run build
Pop-Location

Write-Host '>> 完成。产物：'
Write-Host '   HJ候诊区大屏/dist       -> Nginx /screen'
Write-Host '   HJ诊室门边屏终端/dist   -> Nginx /clinic'
Write-Host '   HJ自助签到终端/dist     -> Nginx /checkin'
