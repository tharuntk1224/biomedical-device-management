# PowerShell Launcher for Biomedical Device Calibration System
$ErrorActionPreference = "Continue"

$javaBin = ""
if (Get-Command java -ErrorAction SilentlyContinue) {
    $javaBin = ""
} elseif (Test-Path "C:\Program Files\Java\jdk-21.0.10\bin\java.exe") {
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
    $javaBin = "C:\Program Files\Java\jdk-21.0.10\bin\"
} else {
    Write-Host "[!] Java not found. Please ensure Java 17+ is installed." -ForegroundColor Red
    exit 1
}

if (Test-Path "target/biomedical-calibration-system-1.0.0.jar") {
    & "${javaBin}java" -jar "target/biomedical-calibration-system-1.0.0.jar"
} elseif (Test-Path "target/classes/com/hospital/biomedical/Main.class") {
    & "${javaBin}java" -cp "target/classes" com.hospital.biomedical.Main
} else {
    Write-Host "[*] Project not compiled yet. Running build first..." -ForegroundColor Yellow
    & ".\build_and_test.ps1"
    & "${javaBin}java" -jar "target/biomedical-calibration-system-1.0.0.jar"
}
