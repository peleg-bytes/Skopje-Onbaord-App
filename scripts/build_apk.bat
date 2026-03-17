@echo off
cd /d "%~dp0..\android-app"
call gradlew.bat assembleRelease
move /Y "app\build\outputs\apk\release\app-release.apk" "app\build\outputs\apk\release\Skopje Survey App.apk" >nul 2>&1
echo.
echo APK output: android-app\app\build\outputs\apk\release\Skopje Survey App.apk
pause
