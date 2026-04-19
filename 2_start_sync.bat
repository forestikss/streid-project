@echo off
chcp 65001 >nul
echo ====================================
echo Запуск автосинхронизации...
echo ====================================
echo.
echo Нажмите Ctrl+C для остановки
echo.

python auto_sync.py

pause
