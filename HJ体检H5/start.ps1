# -*- coding: utf-8 -*-
# 体检H5开发服务器启动脚本

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "    体检H5 开发服务器启动中..." -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "访问地址:" -ForegroundColor Green
Write-Host "  - 本地: http://localhost:5173" -ForegroundColor White
Write-Host "  - 局域网: http://192.168.0.28:5173" -ForegroundColor White
Write-Host ""
Write-Host "按 Ctrl+C 停止服务器" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptPath

npm run dev
