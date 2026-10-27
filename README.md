# Biomedical Device Calibration & Maintenance Management System

> **Enterprise Object-Oriented Java Simulation for Hospital Clinical Engineering**  
> Developed for AI & Data Science College Project Demonstrations and Academic Evaluations.

---

## 1. Project Introduction

The **Biomedical Device Calibration & Maintenance Management System** is a robust, modular Java application designed to emulate clinical engineering and biomedical equipment governance in a modern hospital.

### 🌐 Live HTTP Web Interface
The system includes both an interactive **Console UI** and a modern **Web Dashboard (Single Page Application)** accessible directly via browser:
- **HTTP Link**: [http://localhost:8088/](http://localhost:8088/) or [http://localhost:8080/](http://localhost:8080/)
- **Quick Web Launch**:
  ```powershell
  powershell -ExecutionPolicy Bypass -File .\start_web.ps1
  ```
- Powered by Java's embedded HTTP server (`com.sun.net.httpserver.HttpServer`) with zero external web dependencies.
- Features real-time visual KPI meters, interactive tolerance testing, formal digital calibration certificate viewer, work order kanban tracker, and one-click report exports.

---

## 2. Key Features

- **Biomedical Device Lifecycle Management**:
  - Full support for **Life-Support**, **Diagnostic**, and **Laboratory** equipment.
  - Automatic risk scoring based on clinical severity, battery autonomy, radiation emission, and temperature limits.
  - Decommissioning and equipment retirement with a permanent safety lock preventing accidental reactivation.
- **Dynamic Calibration Engine**:
  - Tolerance verification with precision measurement models:
    $$\text{Deviation} = |\text{Measured Value} - \text{Nominal Value}|$$
    $$\text{Passed} \iff \text{Deviation} \le \text{Allowed Tolerance}$$
  - A calibration test passes *only* if every parameter satisfies tolerances.
  - Generates unique formal certificates (`CERT-YYYY-XXXX`) upon success.
  - **Automated Safety Lockout**: Equipment failing calibration is immediately locked into `OUT_OF_SERVICE`, preventing clinical operation.
  - Dynamic due-date scheduling (`VALID`, `DUE_SOON`, `DUE_TODAY`, `OVERDUE`, `NOT_REQUIRED`).
- **Maintenance Operations & State Machine**:
  - Work order creation for **Preventive**, **Corrective**, and **Emergency** maintenance.
  - Strict legal state transitions: `REQUESTED` $\rightarrow$ `SCHEDULED` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `COMPLETED`.
  - Enforced prevention against reopening closed work orders without authorization.
  - Protection against negative maintenance expenditures and invalid equipment downtime.
  - **Safety Interlock**: Replaced sensors or post-failure maintenance mandate separate calibration recertification before returning to operational status.
- **Hospital Compliance Dashboard (Console KPI)**:
  - Real-time aggregation of active equipment, out-of-service locks, work orders, downtime, and expenditure.
  - Overall calibration compliance KPI calculation:
    $$\text{Compliance } \% = \frac{\text{Valid} + \text{Due Soon}}{\text{Total Requiring Periodic Calibration}} \times 100$$
  - Excludes calibration-exempt equipment from the denominator with zero-division safety.
- **Role-Based Authentication & Cryptographic Hashing**:
  - Secure salted **SHA-256** password hashing (no plaintext credentials).
  - Four distinct roles: `ADMIN`, `TECHNICIAN`, `STAFF`, `AUDITOR`.
  - Service-layer access enforcement.
- **Tamper-Evident Audit Logging**:
  - Automatically records timestamped user actions, parameter adjustments, failed calibrations, and retirement events.
- **Persistence & Reporting**:
  - RFC-4180 compliant file persistence with atomic temp-file swaps (survives sudden shutdowns without data corruption).
  - One-click export to CSV and TXT in the `reports/` directory.

---

## 3. Technology Stack

- **Language**: Java 17 / Java 21 LTS
- **Build & Dependency Management**: Apache Maven (`pom.xml`) + Standalone Zero-Dependency PowerShell Build (`build_and_test.ps1`)
- **Testing Framework**: JUnit 5 Jupiter + Embedded Automated Assertion Test Runner (`StandaloneTestRunner.java`)
- **Date & Currency APIs**: `java.time.LocalDate`, `java.time.LocalDateTime`, `java.math.BigDecimal`
- **Collections**: `ArrayList`, `LinkedHashMap`, `HashMap`, `List`, `Map`
- **Security**: `java.security.MessageDigest` (SHA-256), `SecureRandom`
- **Persistence Format**: Structured Comma-Separated Values (CSV) with atomic file replacement in `data/`

---

## 4. Demonstrating the 4 Pillars of OOP

### 1. Encapsulation
- **Private Fields & Controlled Setters**: All fields across entity classes (`BiomedicalDevice`, `CalibrationMeasurement`, `AbstractMaintenanceRecord`, `User`) are strictly `private`.
- **Domain Validation Rules**:
  - `CalibrationMeasurement.setAllowedTolerance(double)` strictly rejects negative tolerances.
  - `AbstractMaintenanceRecord.setCost(BigDecimal)` prevents negative monetary expenses.
  - `AbstractMaintenanceRecord.setDowntimeHours(double)` rejects negative downtime.
  - `BiomedicalDevice.setDeviceStatus(DeviceStatus)` prevents retired equipment from being reverted back to operational.
  - `User.setUsername(String)` rejects blank strings and normalizes to lowercase.

### 2. Inheritance
The architecture implements clear, logical multi-tier inheritance trees:
- **Base Entity Hierarchy**:
  - `BaseEntity` (abstract ID and timestamps) $\rightarrow$ `Person` (abstract contact details) $\rightarrow$ `User` $\rightarrow$ `Technician`
- **Biomedical Device Hierarchy**:
  - `BaseEntity` $\rightarrow$ `BiomedicalDevice` (abstract)
    - $\rightarrow$ `LifeSupportDevice` (battery backup, redundant alarms)
    - $\rightarrow$ `DiagnosticDevice` (radiation shielding, firmware version)
    - $\rightarrow$ `LaboratoryDevice` (temperature regulation, biosafety level)
- **Maintenance Record Hierarchy**:
  - `BaseEntity` $\rightarrow$ `AbstractMaintenanceRecord` (abstract cost & downtime)
    - $\rightarrow$ `PreventiveMaintenance` (lubrication, cycle intervals)
    - $\rightarrow$ `CorrectiveMaintenance` (root cause, failure categories)
    - $\rightarrow$ `EmergencyMaintenance` (clinical impact, staff informant)

### 3. Polymorphism
- **Dynamic Method Overriding**:
  - `calculateRiskScore()`: Subclasses calculate custom risk scores (`LifeSupportDevice` applies high weights for battery deficits and alarm faults; `DiagnosticDevice` factors radiation exposure).
  - `passesPreCheck()`: Subclasses verify physical pre-conditions before clinical release (`LifeSupportDevice` requires battery $\ge 30$ mins; `LaboratoryDevice` checks temperature boundaries).
- **Overloaded Query Methods (`DeviceRepository` & `DeviceService`)**:
  - `findDevices()` $\rightarrow$ Returns all equipment.
  - `findDevices(String department)` $\rightarrow$ Filters by hospital ward.
  - `findDevices(CalibrationDueStatus status)` $\rightarrow$ Filters by schedule status.
  - `findDevices(DeviceStatus status)` $\rightarrow$ Filters by operational condition.
- **Interface-Based Polymorphism**:
  - Common polymorphic handling through `Calibratable`, `Maintainable`, `Auditable`, and `Reportable`.

### 4. Abstraction
- Entities expose business contracts while hiding underlying calculations.
- Separation of concerns:
  - Models (`model/`) contain only state and entity invariants.
  - Repositories (`repository/`) abstract disk I/O and collection storage.
  - Services (`service/`) encapsulate business rules, authorization, and safety restrictions.
  - `Main.java` orchestrates user interaction without touching raw file streams.

---

## 5. Package Structure

```
java_hospital_management/
├── pom.xml                                      # Maven project configuration
├── README.md                                    # Comprehensive system documentation
├── .gitignore                                   # Git ignore rules
├── build_and_test.ps1                           # Automated compilation and test execution script
├── run.ps1                                      # Console launcher script
├── data/                                        # Local file storage (survives application restarts)
│   ├── devices.csv
│   ├── users.csv
│   ├── calibrations.csv
│   ├── calibration_measurements.csv
│   ├── maintenance_records.csv
│   ├── maintenance_requests.csv
│   └── audit_logs.csv
├── reports/                                     # Exported compliance, inventory & audit reports
│   ├── device_inventory_report.csv
│   ├── calibration_history_report.csv
│   ├── overdue_calibrations_report.csv
│   ├── maintenance_history_report.csv
│   ├── compliance_summary_report.txt
│   └── audit_logs_report.csv
└── src/
    ├── main/java/com/hospital/biomedical/
    │   ├── Main.java                            # Console user interface & application loop
    │   ├── interfaces/
    │   │   ├── Auditable.java
    │   │   ├── Calibratable.java
    │   │   ├── Maintainable.java
    │   │   └── Reportable.java
    │   ├── exception/
    │   │   ├── CalibrationToleranceExceededException.java
    │   │   ├── DeviceNotFoundException.java
    │   │   ├── DeviceOutOfServiceException.java
    │   │   ├── DuplicateEntityException.java
    │   │   ├── InvalidStateTransitionException.java
    │   │   └── UnauthorizedAccessException.java
    │   ├── model/
    │   │   ├── common/
    │   │   │   ├── Address.java
    │   │   │   ├── BaseEntity.java
    │   │   │   └── Person.java
    │   │   ├── device/
    │   │   │   ├── BiomedicalDevice.java
    │   │   │   ├── DeviceCategory.java
    │   │   │   ├── DiagnosticDevice.java
    │   │   │   ├── LaboratoryDevice.java
    │   │   │   └── LifeSupportDevice.java
    │   │   ├── calibration/
    │   │   │   ├── CalibrationCertificate.java
    │   │   │   ├── CalibrationMeasurement.java
    │   │   │   └── CalibrationRecord.java
    │   │   ├── maintenance/
    │   │   │   ├── AbstractMaintenanceRecord.java
    │   │   │   ├── CorrectiveMaintenance.java
    │   │   │   ├── EmergencyMaintenance.java
    │   │   │   ├── MaintenanceRequest.java
    │   │   │   └── PreventiveMaintenance.java
    │   │   ├── user/
    │   │   │   ├── Department.java
    │   │   │   ├── Technician.java
    │   │   │   └── User.java
    │   │   ├── audit/
    │   │   │   └── AuditLog.java
    │   │   └── enums/
    │   │       ├── CalibrationDueStatus.java
    │   │       ├── CalibrationStatus.java
    │   │       ├── DeviceStatus.java
    │   │       ├── MaintenanceStatus.java
    │   │       ├── MaintenanceType.java
    │   │       ├── RiskLevel.java
    │   │       └── UserRole.java
    │   ├── repository/
    │   │   ├── AuditRepository.java
    │   │   ├── CalibrationRepository.java
    │   │   ├── DeviceRepository.java
    │   │   ├── MaintenanceRepository.java
    │   │   ├── TechnicianRepository.java
    │   │   └── UserRepository.java
    │   ├── service/
    │   │   ├── AuditService.java
    │   │   ├── AuthService.java
    │   │   ├── CalibrationService.java
    │   │   ├── ComplianceService.java
    │   │   ├── DataSeeder.java
    │   │   ├── DeviceService.java
    │   │   ├── MaintenanceService.java
    │   │   └── ReportService.java
    │   └── util/
    │       ├── CsvUtils.java
    │       ├── DateUtils.java
    │       ├── IdGenerator.java
    │       ├── SecurityUtils.java
    │       └── TablePrinter.java
    └── test/java/com/hospital/biomedical/
        ├── AuthAndDeviceTest.java               # JUnit 5 test suite
        ├── CalibrationTest.java                 # JUnit 5 test suite
        ├── ComplianceTest.java                  # JUnit 5 test suite
        ├── MaintenanceTest.java                 # JUnit 5 test suite
        ├── PersistenceTest.java                 # JUnit 5 test suite
        ├── StandaloneTestRunner.java            # Zero-dependency test executor
        └── EndToEndWorkflowTest.java            # Section 16 demonstration verification
```

---

## 6. Demonstration Login Accounts

All passwords are automatically salted and hashed with SHA-256.

| Username | Demo Password | Role | Permissions & Responsibilities |
|---|---|---|---|
| `admin` | `Admin@123` | **ADMIN** | Device registration, retirements, user registry, schedule maintenance, export reports |
| `tech1` | `Tech@123` | **TECHNICIAN** | Perform calibration tests, record repairs, parts replacement |
| `tech2` | `Tech@123` | **TECHNICIAN** | Diagnostic & radiation equipment technician |
| `staff1` | `Staff@123` | **STAFF** | View inventory, submit maintenance repair work requests |
| `auditor1` | `Audit@123`| **AUDITOR** | Inspect audit trail, review compliance metrics & reports |

*Quick Tip: During the console login prompt, entering `demo` will automatically log in as `admin`.*

---

## 7. How to Compile, Test, and Run

### Prerequisites
- Windows 10/11 with PowerShell.
- Java JDK 17 or later (installed at `C:\Program Files\Java\jdk-21.0.10` or configured in PATH).

### Windows PowerShell Commands

#### Step 1: Compile the Project & Run Automated Tests
```powershell
powershell -ExecutionPolicy Bypass -File .\build_and_test.ps1
```
This script will:
1. Detect your Java compiler (`javac 21`).
2. Compile all 57 main Java application sources.
3. Compile and execute `StandaloneTestRunner` (executes 14 unit tests).
4. Compile and execute `EndToEndWorkflowTest` (executes the complete Section 16 verification).
5. Package the executable JAR `target/biomedical-calibration-system-1.0.0.jar`.

#### Step 2: Launch the Console Application
```powershell
powershell -ExecutionPolicy Bypass -File .\run.ps1
```

#### Step 3 (Optional): Standard Maven Workflow
If Apache Maven (`mvn`) is installed on your path:
```powershell
mvn clean compile
mvn test
mvn package
java -jar target/biomedical-calibration-system-1.0.0.jar
```

---

## 8. Sample Demonstration Workflows

### Demonstration A: Successful Calibration Workflow
1. Run `.\run.ps1` and log in as `tech1` (`Tech@123`).
2. Choose **7. Perform Calibration Test**.
3. Target device: `DEV-1001` (Mechanical Ventilator).
4. Standard Equipment: `Fluke VT900A`.
5. Enter measurement:
   - Parameter Name: `Tidal Volume`
   - Nominal: `500.0`
   - Measured: `502.1`
   - Unit: `mL`
   - Allowed Tolerance: `15.0`
6. Press Enter on blank parameter name to finish.
7. Observe that the test **PASSES**, generates Certificate `CERT-2026-XXXX`, and updates the device's next calibration date by +180 days.

### Demonstration B: Failed Calibration & Safety Lockout Workflow
1. Choose **7. Perform Calibration Test**.
2. Target device: `DEV-1003` (Patient Monitor).
3. Standard Equipment: `ProSim 8`.
4. Enter measurement:
   - Parameter Name: `ECG Lead Amplitude`
   - Nominal: `1.0`
   - Measured: `1.45`
   - Unit: `mV`
   - Allowed Tolerance: `0.05` (Deviation is $0.45 > 0.05$)
5. Observe the safety result:
   - Test **FAILS**.
   - No certificate is generated.
   - Device status is locked to **`OUT_OF_SERVICE`**.
   - Normal clinical operation is strictly barred.

### Demonstration C: Maintenance Repair & Mandatory Recalibration
1. Choose **9. Create Maintenance Request** as `staff1` (`Staff@123`) for `DEV-1003`.
2. Log in as `admin` (`Admin@123`) and select **10. Approve or Schedule Maintenance**. Assign to `TECH-101`.
3. Log in as `tech1` and select **11. Start Maintenance**.
4. Choose **12. Complete Maintenance**:
   - Cost: `$250.00`
   - Downtime: `3.0` hours
   - Replaced parts: `ECG Motherboard`
5. Observe the crucial clinical engineering safety rule:
   - Completing maintenance **DOES NOT** mark the device operational!
   - The device status transitions to **`CALIBRATION_PENDING`**.
   - A subsequent successful calibration test (Option 7) is mandatory before the device is released for patient care.

### Demonstration D: Compliance Dashboard & Report Generation
1. Select **14. Hospital Compliance Dashboard**:
   - Observe total devices, active vs locked units, overdue calibrations, and overall compliance rate (e.g., $62.5\%$).
2. Select **15. Generate Reports** $\rightarrow$ Option `7` (Export ALL Reports).
3. Inspect generated files in the `reports/` folder (`compliance_summary_report.txt`, `device_inventory_report.csv`, etc.).

---

## 9. Resetting Demonstration Data

To reset the database back to clean baseline demonstration equipment:
1. Log in as `admin`.
2. Choose **17. Manage Users** $\rightarrow$ Option `2` (**Reset Demonstration Accounts**).
3. Or delete all `.csv` files inside the `data/` folder and relaunch `.\run.ps1`. The application will automatically regenerate fresh sample records.

---

## 10. Educational Safety Limitation & Disclaimer

> [!IMPORTANT]
> **SIMULATION NOTICE**: This software is an educational simulation developed for academic instruction in Java Object-Oriented Programming (OOP) and software architecture.  
> It is **NOT** a certified calibration instrument, medical device, or clinical decision support software. It has not been validated under ISO 17025, ISO 13485, FDA 21 CFR Part 820, or IEC 62304 standards. Do not utilize simulated outputs or tolerances for real-world patient diagnosis, therapy, or clinical equipment management.
