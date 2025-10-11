# Python WiFi Server 실행 스크립트
Write-Host "🐍 Python WiFi Server 시작" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Green

# Python이 설치되어 있는지 확인
try {
    $pythonVersion = python --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Python 버전 확인 완료: $pythonVersion" -ForegroundColor Green
    } else {
        throw "Python not found"
    }
} catch {
    Write-Host "❌ Python이 설치되어 있지 않습니다." -ForegroundColor Red
    Write-Host "   Python 3.7 이상을 설치해주세요." -ForegroundColor Yellow
    Read-Host "Enter를 눌러 종료"
    exit 1
}

# Python 서버 실행
Write-Host "🚀 서버 시작 중..." -ForegroundColor Cyan
Write-Host ""

try {
    python python_wifi_server.py
} catch {
    Write-Host "❌ 서버 실행 중 오류 발생: $_" -ForegroundColor Red
} finally {
    Write-Host ""
    Write-Host "🔚 서버가 종료되었습니다." -ForegroundColor Yellow
    Read-Host "Enter를 눌러 종료"
}
