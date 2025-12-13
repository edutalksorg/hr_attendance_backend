@echo off
cd /d C:\Users\ADMIN\Desktop\backend

REM Start the application in background
echo Starting application...
start "MegaMart Backend" cmd /k "java -jar target\backend-0.0.1-SNAPSHOT.jar"

REM Wait for startup
timeout /t 20 /nobreak

REM Run tests
echo.
echo Running tests...
powershell -ExecutionPolicy Bypass -File test-endpoints-simple.ps1

pause
