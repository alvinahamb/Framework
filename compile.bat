@echo off
REM ==== VARIABLES ====
set SRC=src
set LIB=lib
set BUILD=build
set CLASSES=%BUILD%\classes
set DIST=dist
set WAR=%DIST%\kascoco.war

REM ==== CLEAN ====
echo Nettoyage des anciens fichiers...
rmdir /s /q %BUILD%
rmdir /s /q %DIST%

REM ==== CREATION DES REPERTOIRES ====
echo Creation des dossiers...
mkdir %CLASSES%
mkdir %DIST%

REM ==== COMPILATION ====
echo Compilation des fichiers Java...
javac -cp "%LIB%\jakarta.servlet-api-5.0.0.jar" -d %CLASSES% %SRC%\*.java

IF %ERRORLEVEL% NEQ 0 (
    echo Erreur de compilation !
    pause
    exit /b 1
)

REM ==== COPIE DES LIB ET WEB.XML ====
echo Copie des fichiers web...
mkdir %BUILD%\WEB-INF
mkdir %BUILD%\WEB-INF\lib
copy web.xml %BUILD%\WEB-INF\web.xml
copy %LIB%\jakarta.servlet-api.jar %BUILD%\WEB-INF\lib\

REM ==== COPIE DES CLASSES COMPILEES ====
echo Copie des classes compilees...
xcopy %CLASSES% %BUILD%\WEB-INF\classes /E /I /Y

REM ==== GENERATION DU WAR ====
echo Creation du fichier WAR...
cd %BUILD%
jar -cvf ..\%WAR% *
cd ..

echo WAR genere : %WAR%
pause
