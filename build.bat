@echo off
rem Собирает FoldDesk.apk (release, оптимизированный) в корне проекта
set "JAVA_HOME=%USERPROFILE%\.jdks\jdk-17.0.20.1+1"
cd /d "%~dp0"
call gradlew.bat :app:assembleRelease || exit /b 1
copy /y "app\build\outputs\apk\release\app-release.apk" "FoldDesk.apk" >nul
echo Готово: %~dp0FoldDesk.apk
