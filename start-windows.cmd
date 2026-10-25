@echo off
rem Runs scripts\windows\start.ps1. Double-click this file in File Explorer.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\windows\start.ps1" %*
pause
