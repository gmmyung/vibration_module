@echo off
echo 🐍 Python WiFi Server 시작
echo ================================

REM Python이 설치되어 있는지 확인
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Python이 설치되어 있지 않습니다.
    echo    Python 3.7 이상을 설치해주세요.
    pause
    exit /b 1
)

REM Python 서버 실행
echo ✅ Python 버전 확인 완료
echo 🚀 서버 시작 중...
echo.

python python_wifi_server.py

echo.
echo 🔚 서버가 종료되었습니다.
pause
