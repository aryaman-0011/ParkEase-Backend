@echo off
REM ============================================================
REM  ParkEase — Run SonarQube analysis on all microservices
REM  Prerequisites:
REM    1. SonarQube running at http://localhost:9000
REM    2. Create a project token in SonarQube dashboard
REM    3. Pass the token: run-sonar.bat YOUR_SONAR_TOKEN
REM ============================================================

set SONAR_TOKEN=%1
set SONAR_HOST=http://localhost:9000

if "%SONAR_TOKEN%"=="" (
    echo Usage: run-sonar.bat ^<SONAR_TOKEN^>
    echo.
    echo   1. Start SonarQube: docker run -d --name sonarqube -p 9000:9000 sonarqube:lts-community
    echo   2. Open http://localhost:9000, login admin/admin, generate a token
    echo   3. Run: run-sonar.bat ^<your-token^>
    exit /b 1
)

set SERVICES=auth-service booking-service spot-service parkinglot-service payment-service vehicle-service notification-service analytics-service api-gateway

for %%S in (%SERVICES%) do (
    echo.
    echo ============================================================
    echo  Analyzing %%S ...
    echo ============================================================
    cd /d "%~dp0%%S"
    call mvnw.cmd clean verify sonar:sonar -Dsonar.host.url=%SONAR_HOST% -Dsonar.token=%SONAR_TOKEN% -DskipTests=false
    if errorlevel 1 (
        echo [WARN] %%S analysis failed, continuing...
    ) else (
        echo [OK] %%S analysis complete
    )
)

echo.
echo ============================================================
echo  All services analyzed! View results at %SONAR_HOST%
echo ============================================================
cd /d "%~dp0"
