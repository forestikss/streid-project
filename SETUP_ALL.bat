@echo off
chcp 65001 >nul
echo ====================================
echo Полная настройка проекта
echo ====================================
echo.

echo Шаг 1: Добавляем файлы...
git add .

echo.
echo Шаг 2: Создаем коммит...
git commit -m "Initial commit"

echo.
echo Шаг 3: Подключаем GitHub...
echo Вставьте ТОЛЬКО URL (без git clone!)
echo Пример: https://github.com/forestikss/streid-project.git
echo.
set /p repo_url="URL: "

git remote add origin %repo_url%
git push -u origin main

if errorlevel 1 (
    echo Пробуем master...
    git push -u origin master
)

echo.
echo ====================================
echo Готово! Запускайте 2_start_sync.bat
echo ====================================
pause
