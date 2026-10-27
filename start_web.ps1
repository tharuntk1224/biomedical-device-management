# PowerShell Script to Launch Biomedical HTTP Web Interface
$ErrorActionPreference = "Continue"

Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host "   Starting Biomedical Equipment Web Application Server                   " -ForegroundColor Cyan
Write-Host "==========================================================================" -ForegroundColor Cyan

# Locate Java JDK
if (Test-Path "C:\Program Files\Java\jdk-21.0.10\bin\java.exe") {
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
    $javaBin = "$env:JAVA_HOME\bin\java.exe"
} elseif (Get-Command java -ErrorAction SilentlyContinue) {
    $javaBin = "java"
} else {
    Write-Host "[!] Java not found. Please ensure Java 17+ JDK is installed." -ForegroundColor Red
    exit 1
}

# Compile if needed
if (-not (Test-Path "target/classes/com/hospital/biomedical/web/WebServerMain.class")) {
    Write-Host "[*] Compiling application classes..." -ForegroundColor Yellow
    & ".\build_and_test.ps1"
}

Write-Host "[*] Launching Biomedical Web Server..." -ForegroundColor Cyan
Write-Host "[*] Press Ctrl+C to stop the server." -ForegroundColor Yellow
Write-Host ""

& $javaBin -cp "target/classes" com.hospital.biomedical.web.WebServerMain
