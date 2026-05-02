@echo off
chcp 65001 >nul

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5001" ^| findstr "LISTENING"') do (
  echo [信息] 结束进程 PID=%%a
  taskkill /PID %%a /F >nul 2>nul
)

echo [信息] 5001 端口对应服务已尝试停止。
pause
