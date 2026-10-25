@echo off
rem Local dev helper - runs the hands-on playground (dev-tools' HandsOnPlayground): any
rem hands-on level's puzzle on its own, without a server or a game to wait in for one.
setlocal
cd /d "%~dp0"

rem Always reinstall core fresh, not conditionally: dev-tools resolves core from the
rem local Maven repository, so a stale install silently runs old core code.
echo Installing core...
call mvn install -pl core -am -DskipTests -q
if errorlevel 1 (
    echo core failed to build - aborting.
    pause
    exit /b 1
)

rem Let Windows scale the window on a high-DPI display, as start_replayer.cmd does.
set __COMPAT_LAYER=DPIUNAWARE

echo Starting the hands-on playground...
call mvn -pl dev-tools compile exec:exec@handson
pause
