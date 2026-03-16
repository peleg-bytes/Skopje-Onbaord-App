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
    echo No remote 'origin' configured.
    echo.
    set /p REPO_URL="Paste your GitHub repo URL (e.g. https://github.com/username/skopje-onboard-survey.git): "
    if not "%REPO_URL%"=="" (
        git remote add origin "%REPO_URL%"
        echo Remote added.
    ) else (
        echo Skipped. Run: git remote add origin YOUR_REPO_URL
        pause
        exit /b 1
    )
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
