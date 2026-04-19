@echo off
chcp 65001 >nul
echo ====================================
echo Проверка установленных программ
echo ====================================
echo.

echo Проверяем Python...
python --version
if errorlevel 1 (
    echo ❌ Python НЕ установлен!
    echo Скачай: https://www.python.org/downloads/
) else (
    echo ✓ Python установлен
)

echo.
echo Проверяем pip...
pip --version
if errorlevel 1 (
    echo ❌ pip НЕ установлен!
) else (
    echo ✓ pip установлен
)

echo.
echo Проверяем Git...
git --version
if errorlevel 1 (
    echo ❌ Git НЕ установлен!
    echo Скачай: https://git-scm.com/download/win
) else (
    echo ✓ Git установлен
)

echo.
echo ====================================
echo Проверка завершена
echo ====================================
pause
