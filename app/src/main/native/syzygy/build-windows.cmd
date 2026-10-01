@echo off
setlocal
where cl >nul 2>nul
if not errorlevel 1 goto compile
set "SEED_VSWHERE=%ProgramFiles(x86)%\Microsoft Visual Studio\Installer\vswhere.exe"
if not exist "%SEED_VSWHERE%" exit /b 1
for /f "usebackq tokens=*" %%i in (`"%SEED_VSWHERE%" -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath`) do set "SEED_VS=%%i"
if not defined SEED_VS exit /b 1
call "%SEED_VS%\VC\Auxiliary\Build\vcvars64.bat" >nul
if errorlevel 1 exit /b %errorlevel%
:compile
cl /nologo /O2 /LD /TP /std:c++17 /EHsc /DTB_NO_THREADS /DTB_NO_HW_POP_COUNT /I"%~1\include" /I"%~1\include\win32" /I"%~3\fathom" "%~3\fathom\tbprobe.c" "%~3\seedv6_syzygy.cpp" /Fe:"%~2\seedv6_syzygy.dll"
exit /b %errorlevel%
