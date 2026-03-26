@echo off
setlocal EnableExtensions
cd /d "%~dp0..\android-app"

if not exist "keystore.properties" (
  echo [ERROR] android-app\keystore.properties not found.
  echo Copy keystore.properties.example to keystore.properties and place release.keystore in android-app\
  exit /b 1
)

echo.
echo === Signed release APK ===
echo Gradle: daemon ^(default^), parallel + build cache from gradle.properties
echo.

call gradlew.bat assembleRelease --build-cache
set "ERR=%ERRORLEVEL%"
if not "%ERR%"=="0" (
  echo.
  echo [ERROR] assembleRelease failed ^(exit %ERR%^)
  exit /b %ERR%
)

set "APK=%CD%\app\build\outputs\apk\release\app-release.apk"
if not exist "%APK%" (
  echo [ERROR] Expected APK missing: %APK%
  exit /b 1
)

echo.
echo OK — signed release APK:
echo   %APK%
echo.

if /I not "%~1"=="nopause" (
  if /I not "%CI%"=="true" pause
)

exit /b 0
