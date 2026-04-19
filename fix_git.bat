@echo off
chcp 65001 >nul
echo ====================================
echo Исправление Git настроек
echo ====================================
echo.

echo Удаляем старый remote...
git remote remove origin

echo.
echo Проверяем статус...
git status

echo.
echo Добавляем все файлы...
git add .

echo.
echo Создаем коммит...
git commit -m "Initial commit"

echo.
set /p repo_url="Введите URL репозитория: "

echo.
echo Добавляем remote...
git remote add origin %repo_url%

echo.
echo Отправляем на GitHub...
git push -u origin master

if errorlevel 1 (
    echo.
    echo Пробуем с main...
    git branch -M main
    git push -u origin main
)

echo.
echo ====================================
echo Готово!
echo ====================================
pause
