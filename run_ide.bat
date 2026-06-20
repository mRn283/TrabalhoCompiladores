@echo off
title CompilaMeme IDE
echo ===========================================
echo 1. Compilando os fontes do CompilaMeme...
echo ===========================================
javac -d bin src/pacotememe/*.java

if %errorlevel% neq 0 (
    echo.
    echo [ERRO] Falha na compilacao dos arquivos Java.
    echo Certifique-se de que o JDK - Java Development Kit - esta instalado e configurado no seu PATH.
    echo.
    pause
    exit /b 1
)

echo.
echo ===========================================
echo 2. Iniciando a IDE...
echo ===========================================
java -cp bin pacotememe.gramaticameme

if %errorlevel% neq 0 (
    echo.
    echo [AVISO] A IDE fechou ou encontrou um erro - Codigo: %errorlevel%.
)

echo.
echo Pressione qualquer tecla para fechar esta janela.
pause > nul
