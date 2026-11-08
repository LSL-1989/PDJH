@echo off
chcp 65001 >nul
title 体检H5开发服务器
echo ========================================
echo       体检H5 开发服务器启动中...
echo ========================================
echo.
echo 访问地址:
echo   - 本地: http://localhost:5173
echo   - 局域网: http://192.168.0.28:5173
echo.
echo 按 Ctrl+C 停止服务器
echo ========================================
echo.

cd /d "%~dp0"
echo 正在启动开发服务器...
call npx --yes vite@latest --host 0.0.0.0 --port 5173
pause
