@echo off
chcp 65001 >nul
echo ====================================
echo Настройка Git репозитория
echo ====================================
echo.

echo Шаг 1: Инициализация Git...
git init

echo.
echo Шаг 2: Добавление файлов...
git add .

echo.
echo Шаг 3: Первый коммит...
git commit -m "Initial commit"

echo.
echo ====================================
echo ВАЖНО! Теперь сделайте следующее:
echo ====================================
echo 1. Создайте репозиторий на GitHub.com
echo 2. Скопируйте URL репозитория
echo 3. Введите его ниже
echo.

set /p repo_url="Введите URL репозитория (например, https://github.com/username/repo.git): "

echo.
echo Подключение к удаленному репозиторию...
git remote add origin %repo_url%
git branch -M main
git push -u origin main

if errorlevel 1 (
    echo.
    echo Пробуем с веткой master...
    git branch -M master
    git push -u origin master
)

echo.
echo ====================================
echo Готово! Теперь запустите 2_start_sync.bat
echo ====================================
pause
