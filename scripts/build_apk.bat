@echo off
cd /d "%~dp0..\android-app"
call gradlew.bat assembleRelease
echo.
echo APK output: android-app\app\build\outputs\apk\release\app-release.apk
pause
