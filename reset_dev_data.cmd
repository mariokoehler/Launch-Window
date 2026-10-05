@echo off
rem Local dev helper - resets the dev game, so the next server start begins a fresh game
rem (needed whenever the content pack or a save format changes; see CLAUDE.md).
rem
rem Deletes everything in the server's data folder except game-meta.json (the game's
rem settings, dev mode included) and accounts.json (the logins, so the client's
rem remembered login still works). Also clears the client's "report read" marks,
rem which would otherwise treat the new game's first turns as already read.
rem
rem   reset_dev_data.cmd        asks before deleting
rem   reset_dev_data.cmd /y     doesn't ask
rem   reset_dev_data.cmd /all   also deletes accounts.json (everyone signs up again)
setlocal
cd /d "%~dp0"
set "DATA=data"
set "PREFS=%USERPROFILE%\.prefs\launchwindow"
set "ALL="
set "YES="
for %%a in (%*) do (
    if /i "%%a"=="/all" set "ALL=1"
    if /i "%%a"=="/y" set "YES=1"
)

if not exist "%DATA%\" (
    echo There is no data folder - nothing to reset.
    pause
    exit /b 0
)

echo This resets the dev game in "%DATA%":
echo   - deleted: players, reports, the frozen content pack, the scheduler state,
echo     the game result
echo   - kept:    game-meta.json, the game's settings
if defined ALL (
    echo   - deleted: accounts.json, so everyone signs up again
) else (
    echo   - kept:    accounts.json, so logins still work - use /all to delete it too
)
echo   - cleared: the client's report read marks
echo.
echo Stop the server first: a running server would write its old game back.
echo.
if not defined YES (
    choice /c YN /m "Reset now"
    if errorlevel 2 goto cancelled
)

for /d %%d in ("%DATA%\*") do rmdir /s /q "%%d"
for %%f in ("%DATA%\*") do call :deleteFile "%%f"
if exist "%PREFS%" del /q "%PREFS%"

echo.
echo Done. The next server start begins a fresh game.
pause
exit /b 0

:deleteFile
if /i "%~nx1"=="game-meta.json" exit /b 0
if /i "%~nx1"=="accounts.json" if not defined ALL exit /b 0
del /q "%~1"
exit /b 0

:cancelled
echo Cancelled - nothing was deleted.
pause
exit /b 1
