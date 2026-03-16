@echo off
cd /d "%~dp0.."

if not exist .git (
    echo Initializing git repository...
    git init
    echo.
)

echo Adding all files...
git add .

echo.
echo Current status:
git status

echo.
set /p COMMIT_MSG="Enter commit message (or press Enter for default): "
if "%COMMIT_MSG%"=="" set "COMMIT_MSG=Update Skopje Onboard Survey"

echo.
echo Committing: %COMMIT_MSG%
git commit -m "%COMMIT_MSG%"

if %ERRORLEVEL% neq 0 (
    echo.
    echo No changes to commit, or commit failed.
)

echo.
git remote show origin >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo Adding remote origin: https://github.com/peleg-bytes/Skopje-Onbaord-App.git
    git remote add origin https://github.com/peleg-bytes/Skopje-Onbaord-App.git
    echo Remote added.
)

echo.
echo Pushing to GitHub...
git branch -M main 2>nul
git push -u origin main

if %ERRORLEVEL% neq 0 (
    echo.
    echo Push failed. You may need to:
    echo   1. Create a repo on GitHub first
    echo   2. Run: git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
    echo   3. Authenticate (GitHub may prompt for credentials or token)
)

echo.
pause
