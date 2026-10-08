@REM ----------------------------------------------------------------------------
@REM Maven Wrapper script for Windows
@REM ----------------------------------------------------------------------------
@echo off
setlocal

set "DIR=%~dp0"
set "MAVEN_CMD=mvn"

where mvn >nul 2>nul
if %ERRORLEVEL% equ 0 (
    mvn %*
    exit /b %ERRORLEVEL%
)

if exist "%M2_HOME%\bin\mvn.cmd" (
    "%M2_HOME%\bin\mvn.cmd" %*
    exit /b %ERRORLEVEL%
)

if exist "%MAVEN_HOME%\bin\mvn.cmd" (
    "%MAVEN_HOME%\bin\mvn.cmd" %*
    exit /b %ERRORLEVEL%
)

echo [ERROR] Apache Maven was not found on your PATH, M2_HOME, or MAVEN_HOME.
echo [ERROR] Please install Apache Maven (https://maven.apache.org/) and Java 21 to run mvnw.cmd.
exit /b 1
