@echo off
chcp 65001 >nul
title 体检H5开发服务器
echo ========================================
echo       体检H5 开发服务器启动中...
echo ========================================
echo.

cd /d "%~dp0"

echo 检查node_modules...
if not exist node_modules (
    echo node_modules不存在,正在安装依赖...
    call npm install --legacy-peer-deps
    if errorlevel 1 (
        echo 安装依赖失败,尝试使用npx直接运行...
        echo.
    )
)

echo.
echo 访问地址:
echo   - 本地: http://localhost:5173
echo   - 局域网: http://192.168.0.28:5173
echo.
echo 按 Ctrl+C 停止服务器
echo ========================================
echo.

if exist node_modules\.bin\vite.cmd (
    echo 使用本地vite启动...
    call node_modules\.bin\vite.cmd --host 0.0.0.0 --port 5173
) else (
    echo 使用npx启动vite...
    call npx vite@latest --host 0.0.0.0 --port 5173
)

pause
