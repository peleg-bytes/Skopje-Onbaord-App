@echo off
cd /d "%~dp0..\backend"
call vercel deploy --prod
echo.
echo Backend deployed. Update API URL in Android app Settings if the URL changed.
pause
