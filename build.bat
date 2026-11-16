@echo off
setlocal
where gradle >nul 2>nul
if errorlevel 1 (
  echo Gradle is not installed or not on PATH.
  echo Install Gradle 8.x and Java 17, then run this file again.
  pause
  exit /b 1
)
gradle build
if errorlevel 1 (
  echo Build failed. Copy the error text and send it for troubleshooting.
  pause
  exit /b 1
)
echo.
echo Build complete. JAR files are in build\libs\
explorer build\libs
pause
