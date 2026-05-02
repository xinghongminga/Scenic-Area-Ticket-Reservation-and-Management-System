@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"

set "PY=.\.venv\Scripts\python.exe"
if not exist "%PY%" (
  echo [错误] 未找到虚拟环境 Python: %PY%
  pause
  exit /b 1
)

if not "%YOLO_MODEL_PATH%"=="" (
  echo [信息] 使用本地模型: %YOLO_MODEL_PATH%
)

echo [信息] 正在启动 Python 检测服务...
"%PY%" -m uvicorn app.main:app --host 0.0.0.0 --port 5001

echo.
echo [信息] 服务已退出。
pause
