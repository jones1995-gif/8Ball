@echo off
setlocal
set "GRADLE_VERSION=8.9"
set "CACHE_BASE=%USERPROFILE%\.gradle\manual-distributions"
set "GRADLE_HOME=%CACHE_BASE%\gradle-%GRADLE_VERSION%"
set "ZIP_PATH=%CACHE_BASE%\gradle-%GRADLE_VERSION%-bin.zip"
if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  if not exist "%CACHE_BASE%" mkdir "%CACHE_BASE%"
  if not exist "%ZIP_PATH%" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.9-bin.zip' -OutFile '%ZIP_PATH%'"
    if errorlevel 1 exit /b 1
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$actual=(Get-FileHash -Algorithm SHA256 '%ZIP_PATH%').Hash.ToLower(); if ($actual -ne 'd725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab') { Write-Error 'Checksum do Gradle inválido'; exit 1 }; Expand-Archive -Path '%ZIP_PATH%' -DestinationPath '%CACHE_BASE%' -Force"
  if errorlevel 1 exit /b 1
)
call "%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
