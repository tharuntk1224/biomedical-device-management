$ErrorActionPreference = "Continue"

Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host "   Compiling Biomedical Device Calibration & Maintenance System           " -ForegroundColor Cyan
Write-Host "==========================================================================" -ForegroundColor Cyan

# Locate Java JDK
if (Test-Path "C:\Program Files\Java\jdk-21.0.10\bin\javac.exe") {
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
    $javaBin = "$env:JAVA_HOME\bin\"
} elseif (Get-Command javac -ErrorAction SilentlyContinue) {
    $javaBin = ""
} else {
    Write-Host "[!] Javac not found. Please ensure Java 17+ JDK is installed." -ForegroundColor Red
    exit 1
}

$v = (& "${javaBin}java.exe" -version 2>&1)
Write-Host "[*] Using Java: $($v[0])" -ForegroundColor Green


# Prepare target build directories
$buildDir = "target/classes"
$testBuildDir = "target/test-classes"
if (Test-Path "target") { Remove-Item -Recurse -Force "target" }
New-Item -ItemType Directory -Force -Path $buildDir | Out-Null
New-Item -ItemType Directory -Force -Path $testBuildDir | Out-Null
New-Item -ItemType Directory -Force -Path "data" | Out-Null
New-Item -ItemType Directory -Force -Path "reports" | Out-Null

# Find all Java source files
$mainSources = Get-ChildItem -Path "src/main/java" -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }
$testSources = @(
    (Get-Item "src/test/java/com/hospital/biomedical/StandaloneTestRunner.java").FullName,
    (Get-Item "src/test/java/com/hospital/biomedical/EndToEndWorkflowTest.java").FullName
)

Write-Host "[*] Compiling $($mainSources.Count) main application sources..." -ForegroundColor Yellow
& "${javaBin}javac" -encoding UTF-8 -d $buildDir $mainSources
if ($LASTEXITCODE -ne 0) {
    Write-Host "[!] Compilation failed." -ForegroundColor Red
    exit $LASTEXITCODE
}
Write-Host "[+] Main sources compiled successfully!" -ForegroundColor Green

# Copy resources if present
if (Test-Path "src/main/resources") {
    Copy-Item -Path "src/main/resources/*" -Destination $buildDir -Recurse -Force
    Write-Host "[+] Resources copied to $buildDir" -ForegroundColor Green
}

Write-Host "[*] Compiling Automated Test Suites..." -ForegroundColor Yellow
& "${javaBin}javac" -encoding UTF-8 -cp $buildDir -d $testBuildDir $testSources
if ($LASTEXITCODE -ne 0) {
    Write-Host "[!] Test runner compilation failed." -ForegroundColor Red
    exit $LASTEXITCODE
}
Write-Host "[+] Test sources compiled successfully!" -ForegroundColor Green

Write-Host ""
Write-Host "[*] Executing Core Automated Unit Tests..." -ForegroundColor Cyan
& "${javaBin}java" -ea -cp "$buildDir;$testBuildDir" com.hospital.biomedical.StandaloneTestRunner
if ($LASTEXITCODE -ne 0) {
    Write-Host "[!] Core tests failed." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host ""
Write-Host "[*] Executing End-to-End Demonstration Workflow..." -ForegroundColor Cyan
& "${javaBin}java" -ea -cp "$buildDir;$testBuildDir" com.hospital.biomedical.EndToEndWorkflowTest
if ($LASTEXITCODE -ne 0) {
    Write-Host "[!] End-to-End workflow test failed." -ForegroundColor Red
    exit $LASTEXITCODE
}

# Package JAR file
Write-Host ""
Write-Host "[*] Packaging executable JAR file..." -ForegroundColor Yellow
$manifestContent = "Main-Class: com.hospital.biomedical.Main`n"
Set-Content -Path "target/manifest.mf" -Value $manifestContent -Encoding ASCII
& "${javaBin}jar" -cfm "target/biomedical-calibration-system-1.0.0.jar" "target/manifest.mf" -C $buildDir .
Write-Host "[+] JAR created at target/biomedical-calibration-system-1.0.0.jar" -ForegroundColor Green

Write-Host ""
Write-Host "==========================================================================" -ForegroundColor Green
Write-Host "   BUILD & TESTS COMPLETED WITH 100% SUCCESS!                             " -ForegroundColor Green
Write-Host "   To start the application, run: .\run.ps1                                " -ForegroundColor Green
Write-Host "==========================================================================" -ForegroundColor Green
