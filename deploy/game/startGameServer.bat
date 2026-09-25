@echo off
title Game Server Console
:start
echo Starting L2J Game Server.
echo.
REM -------------------------------------
java -Xms256m -Xmx512m -jar l2jserver.jar
REM -------------------------------------
if ERRORLEVEL 2 goto restart
if ERRORLEVEL 1 goto error
goto end
:restart
echo.
echo Admin Restart ...
echo.
goto start
:error
echo.
echo Server terminated abnormally
echo.
:end
echo.
echo server terminated
echo.
pause
