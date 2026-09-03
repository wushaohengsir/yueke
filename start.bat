@echo off
chcp 65001 >nul
title BookMate 一键启动

set WS=C:\Users\13681\.qwenworkcn\workspace\mth2ugx9m2ni92i0
set PATH=%WS%\node;%WS%\jdk-17.0.2\bin;%PATH%

echo ==========================================
echo   通用师生约课平台 BookMate - 一键启动
echo ==========================================
echo.

REM ---- MySQL ----
netstat -ano | findstr ":3306" | findstr "LISTENING" >nul
if errorlevel 1 (
    echo [1/3] MySQL 启动中...
    start "BookMate-MySQL" /D "%WS%\mysql-8.0.28-winx64" "%WS%\mysql-8.0.28-winx64\bin\mysqld.exe" --console
) else (
    echo [1/3] MySQL 已在运行，跳过
)

REM ---- 后端 ----
echo [2/3] 后端 Spring Boot 启动中 (8080)...
start "BookMate-Backend" cmd /k "cd /d d:\yueke\yueke\backend && java -jar target\backend-0.1.0.jar"

REM ---- 前端 ----
echo [3/3] 前端 Vite 启动中 (5173)...
start "BookMate-Frontend" cmd /k "cd /d d:\yueke\yueke\frontend && npm run dev -- --port 5173 --host"

echo.
echo 已发起启动，请等待各窗口就绪（后端约 20-40 秒）。
echo 访问地址: http://localhost:5173
echo 提示: 关闭对应窗口即可停止该服务。
echo.
pause
