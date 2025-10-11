# PowerShell script to clean up build artifacts and prevent file lock issues

Write-Host "Cleaning up build artifacts to prevent file lock issues..." -ForegroundColor Green

# Kill any running Java processes (Gradle daemons)
try {
    Get-Process -Name "java" -ErrorAction SilentlyContinue | Stop-Process -Force
    Write-Host "Stopped Java processes" -ForegroundColor Yellow
} catch {
    Write-Host "No Java processes to stop" -ForegroundColor Gray
}

# Clean Gradle cache
$gradleCache = "$env:USERPROFILE\.gradle\caches"
if (Test-Path $gradleCache) {
    Write-Host "Cleaning Gradle cache..." -ForegroundColor Yellow
    Remove-Item -Path $gradleCache -Recurse -Force -ErrorAction SilentlyContinue
}

# Clean project build directories
if (Test-Path "build") {
    Write-Host "Cleaning project build directory..." -ForegroundColor Yellow
    Remove-Item -Path "build" -Recurse -Force -ErrorAction SilentlyContinue
}

if (Test-Path "app\build") {
    Write-Host "Cleaning app build directory..." -ForegroundColor Yellow
    Remove-Item -Path "app\build" -Recurse -Force -ErrorAction SilentlyContinue
}

# Clean .gradle directory
if (Test-Path ".gradle") {
    Write-Host "Cleaning .gradle directory..." -ForegroundColor Yellow
    Remove-Item -Path ".gradle" -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host "Cleanup completed successfully!" -ForegroundColor Green
Write-Host "You can now run your build commands." -ForegroundColor Cyan
