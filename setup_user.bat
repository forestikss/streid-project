@echo off
chcp 65001 >nul
echo ====================================
echo Настройка пользователя Git
echo ====================================
echo.

set /p username="Введите ваше имя (например, Ivan): "
set /p email="Введите ваш email (например, ivan@mail.ru): "

echo.
echo Настраиваем Git...
git config --global user.name "%username%"
git config --global user.email "%email%"

echo.
echo ====================================
echo Готово! Теперь запустите fix_git.bat
echo ====================================
pause
