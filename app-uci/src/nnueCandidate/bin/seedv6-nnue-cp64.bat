@echo off
setlocal
set "APP_HOME=%~dp0.."
set "JAVA_EXE=java.exe"
if defined JAVA_HOME set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if exist "%APP_HOME%\runtime\bin\java.exe" set "JAVA_EXE=%APP_HOME%\runtime\bin\java.exe"
set "VECTOR="
"%JAVA_EXE%" --list-modules 2>nul | findstr /b /c:"jdk.incubator.vector@" >nul
if not errorlevel 1 set "VECTOR=--add-modules=jdk.incubator.vector"
"%JAVA_EXE%" %VECTOR% -XX:ActiveProcessorCount=2 -Xmx768m -Djava.awt.headless=true -cp "%APP_HOME%\lib\*" com.ohinteractive.seedv6.uci.NnueCandidateUciMain %*
exit /b %ERRORLEVEL%
