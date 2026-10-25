@echo off
rem Runs scripts\windows\stop.ps1. Double-click this file in File Explorer.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\windows\stop.ps1" %*
pause
