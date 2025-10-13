# Web Visualizer Server 실행 스크립트
Write-Host "🌐 Web Visualizer Server 시작" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Green

# Node.js가 설치되어 있는지 확인
try {
    $nodeVersion = node --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Node.js 버전 확인 완료: $nodeVersion" -ForegroundColor Green
    } else {
        throw "Node.js not found"
    }
} catch {
    Write-Host "❌ Node.js가 설치되어 있지 않습니다." -ForegroundColor Red
    Write-Host "   Node.js 16 이상을 설치해주세요." -ForegroundColor Yellow
    Read-Host "Enter를 눌러 종료"
    exit 1
}

# npm 의존성 확인
Write-Host "📦 의존성 확인 중..." -ForegroundColor Cyan
try {
    npm list express > $null 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "📥 Express 의존성 설치 중..." -ForegroundColor Yellow
        npm install
    }
} catch {
    Write-Host "❌ 의존성 확인 중 오류 발생" -ForegroundColor Red
    Read-Host "Enter를 눌러 종료"
    exit 1
}

# 서버 실행
Write-Host "🚀 서버 시작 중..." -ForegroundColor Cyan
Write-Host "   - TCP 서버: 포트 8888 (ESP32 앱용)" -ForegroundColor White
Write-Host "   - WebSocket 서버: 포트 8888 (웹 클라이언트용)" -ForegroundColor White
Write-Host "   - Express API 서버: 포트 3001 (WiFi IP API용)" -ForegroundColor White
Write-Host "   - React 앱: 포트 3000 (웹 시각화)" -ForegroundColor White
Write-Host ""

try {
    # 백그라운드에서 서버 실행
    Start-Process -FilePath "node" -ArgumentList "src/server/brailleServer.js" -WindowStyle Hidden
    
    # 잠시 대기
    Start-Sleep -Seconds 2
    
    # React 앱 실행
    Write-Host "🌐 React 앱 시작 중..." -ForegroundColor Cyan
    npm start
} catch {
    Write-Host "❌ 서버 실행 중 오류 발생: $_" -ForegroundColor Red
} finally {
    Write-Host ""
    Write-Host "🔚 서버가 종료되었습니다." -ForegroundColor Yellow
    Read-Host "Enter를 눌러 종료"
}
