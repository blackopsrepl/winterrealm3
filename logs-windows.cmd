@echo off
rem Runs scripts\windows\logs.ps1. Double-click this file in File Explorer.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\windows\logs.ps1" %*
pause
