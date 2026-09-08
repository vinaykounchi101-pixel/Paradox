@echo off
setlocal enabledelayedexpansion

echo ====================================================
echo 📱 PARADOX 1-CLICK USB INSTALLER
echo ====================================================

set "APK_PATH=%~dp0..\app\build\outputs\apk\debug\app-debug.apk"

if not exist "!APK_PATH!" (
    echo [ERROR] Compiled APK not found at: !APK_PATH!
    echo Please build the APK first.
    exit /b 1
)

:: Check if adb is in PATH, local tools folder, or standard Android SDK locations
if exist "%~dp0platform-tools\adb.exe" (
    set "ADB_CMD=%~dp0platform-tools\adb.exe"
) else if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
    set "ADB_CMD=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
) else (
    where adb >nul 2>&1
    if !ERRORLEVEL! equ 0 (
        set "ADB_CMD=adb"
    ) else (

    echo [WARNING] 'adb' not found in PATH or standard SDK location.
    echo Searching system for adb...
    for /f "delims=" %%i in ('where /r "C:\Users\%USERNAME%" adb.exe 2^>nul') do (
        set "ADB_CMD=%%i"
        goto :found_adb
    )
    echo [ERROR] Could not find adb.exe. Please ensure USB Debugging is ON and adb is installed.
    exit /b 1
)

:found_adb
echo Found ADB: !ADB_CMD!
echo Checking connected devices...
!ADB_CMD! devices

echo.
echo Installing Paradox to connected device...
!ADB_CMD! install -r -d "!APK_PATH!"

if %ERRORLEVEL% equ 0 (
    echo.
    echo ====================================================
    echo [SUCCESS] Paradox installed successfully on your phone!
    echo Launching Paradox...
    echo ====================================================
    !ADB_CMD! shell am start -n com.paradox.finance.debug/com.paradox.finance.MainActivity
) else (
    echo.
    echo [FAILED] Installation failed. Ensure your phone screen is unlocked and USB Debugging is authorized.
)
