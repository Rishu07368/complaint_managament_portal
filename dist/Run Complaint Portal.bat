@echo off
setlocal
cd /d "%~dp0"

if not exist "ComplaintPortal\application.properties" (
    echo Missing ComplaintPortal\application.properties.
    echo Copy application.properties.example to ComplaintPortal\application.properties and configure MySQL.
    pause
    exit /b 1
)

start "" "ComplaintPortal\ComplaintPortal.exe"
