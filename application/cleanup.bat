@echo off
echo Cleaning up build artifacts to prevent file lock issues...

REM Kill any running Java processes (Gradle daemons)
taskkill /f /im java.exe 2>nul

REM Clean Gradle cache
if exist "%USERPROFILE%\.gradle\caches" (
    echo Cleaning Gradle cache...
    rmdir /s /q "%USERPROFILE%\.gradle\caches" 2>nul
)

REM Clean project build directories
if exist "build" (
    echo Cleaning project build directory...
    rmdir /s /q "build" 2>nul
)

if exist "app\build" (
    echo Cleaning app build directory...
    rmdir /s /q "app\build" 2>nul
)

REM Clean .gradle directory
if exist ".gradle" (
    echo Cleaning .gradle directory...
    rmdir /s /q ".gradle" 2>nul
)

echo Cleanup completed successfully!
echo You can now run your build commands.
pause
