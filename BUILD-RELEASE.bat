@echo off
setlocal
cd /d "%~dp0"
echo Building AptiRise production release 1.1.2 ^(13^)...
call gradlew.bat :app:testDebugUnitTest :app:assembleRelease :app:bundleRelease
if errorlevel 1 (
    echo.
    echo Build or tests failed. Review the Gradle error above.
    pause
    exit /b 1
)
echo.
echo APK: app\build\outputs\apk\release\app-release.apk
echo Play bundle: app\build\outputs\bundle\release\app-release.aab
echo Release uses your production ad units.
pause
