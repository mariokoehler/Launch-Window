@echo off
rem Local dev helper - runs the mission replayer (dev-tools' MissionReplayer): any
rem mission's replay with any outcome, without a server or a game to play up to it.
rem Pass a content pack to use another than the standard one: start_replayer.cmd early-era
setlocal
cd /d "%~dp0"

set PACK=%~1
if "%PACK%"=="" set PACK=standard

rem Always reinstall core fresh, not conditionally: dev-tools resolves core from the
rem local Maven repository, so a stale install silently runs old core code.
echo Installing core...
call mvn install -pl core -am -DskipTests -q
if errorlevel 1 (
    echo core failed to build - aborting.
    pause
    exit /b 1
)

echo Starting the mission replayer (%PACK%)...
call mvn -pl dev-tools compile exec:exec@replayer -Dreplayer.pack=%PACK%
pause
