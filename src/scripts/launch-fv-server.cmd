@echo off

rem simple script to launch fv programs

set ERROR_CODE=0
setlocal ENABLEEXTENSIONS

rem hard-code FV_HOME, modify next line as needed
rem set FV_HOME=C:\fv

rem use environment variable, if defined
if "%FV_HOME%"=="" goto LABEL_NOT_DEFINED

cd "%FV_HOME%"
bin\fv-server
goto LABEL_EXIT

:LABEL_NOT_DEFINED
echo FV_HOME is NOT defined, exiting!
pause
goto LABEL_EXIT

:LABEL_EXIT