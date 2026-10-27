# MINI PROJECT PRESENTATION: SLIDE-BY-SLIDE CONTENT
## PROJECT TITLE: Biomedical Device Calibration and Maintenance Management System
### Following Official Academic Mini Project Presentation Guidelines

---

## SLIDE 1: TITLE PAGE

### [Slide Title]
**BIOMEDICAL DEVICE CALIBRATION AND MAINTENANCE MANAGEMENT SYSTEM**

### [Slide Subtitle]
An Enterprise Java Object-Oriented Simulation for Hospital Clinical Engineering Governance

### [Slide Content / Details]
* **Subject Code & Title:** [Insert Subject Code, e.g., CS8591 – Object-Oriented Analysis and Design / Mini Project Lab]
* **Project Title:** Biomedical Device Calibration and Maintenance Management System
* **Team Members & Register Numbers:**
  * Member 1: [Name] – Reg No: [12345678]
  * Member 2: [Name] – Reg No: [12345678]
  * Member 3: [Name] – Reg No: [12345678]
* **Project Guide:** [Guide / Faculty Name, Designation]
* **Department:** Department of Computer Science & Engineering / Artificial Intelligence & Data Science
* **Institution:** [College / University Name]
* **Academic Year:** 2025 – 2026

---

## SLIDE 2: ABSTRACT

### [Slide Title]
**ABSTRACT**

### [Slide Content]
* **Clinical Significance:** Modern healthcare facilities rely on complex biomedical equipment (ventilators, defibrillators, patient monitors). Even micro-deviations in device calibration can cause fatal clinical errors or diagnostic failure.
* **The Problem:** Hospital biomedical engineering units often struggle with decentralized logbooks, missed calibration intervals, lack of automated safety locks, and high maintenance downtime.
* **Proposed Solution:** 
  * A robust, modular Java software system built on **Clean Layered Architecture** and the **4 Pillars of Object-Oriented Programming (OOP)**.
  * Embeds an automated mathematical calibration tolerance verification engine that calculates deviations in real-time.
* **Core Safety Interlocks:**
  * **Automatic Lockout:** Devices failing calibration are instantly locked into `OUT_OF_SERVICE`.
  * **Mandatory Recertification:** Post-repair devices are held in `CALIBRATION_PENDING` and cannot be clinically deployed until re-tested.
* **Architecture & Delivery:** Zero-dependency crash-resilient CSV persistence with atomic temp-file swaps, backed by both an ANSI terminal interface and a browser-based Single Page Application (SPA) Web Dashboard.

---

## SLIDE 3: INTRODUCTION AND PROBLEM STATEMENT

### [Slide Title]
**INTRODUCTION AND PROBLEM STATEMENT**

### [Slide Content]
#### 1. Introduction:
* Biomedical engineering governance mandates that diagnostic and therapeutic devices comply with international healthcare standards (**ISO 13485**, **ISO 17025**, **IEC 60601**).
* Hospital clinical teams require real-time visibility into equipment calibration validity, risk ratings, and maintenance work orders.

#### 2. Problem Statement:
* **Manual Tolerance Calculations:** Technicians manually verify measured readings against nominal values, introducing human arithmetic error.
* **Absence of Immediate Safety Interlocks:** If a defibrillator fails energy discharge testing, conventional manual logging creates a dangerous delay before the machine is physically removed from the emergency ward.
* **The "Fixed = Operational" Fallacy:** Conventional systems allow technicians to mark a repaired device as "operational" without verifying that replacement parts (e.g., pressure sensors) have been calibrated.
* **Security & Audit Deficits:** Plaintext logs fail regulatory audits, lack non-repudiation, and do not track chronological user adjustments.

---

## SLIDE 4: OBJECTIVES

### [Slide Title]
**PROJECT OBJECTIVES**

### [Slide Content]
* **Primary Objective:** Develop an end-to-end Biomedical Device Calibration and Maintenance Management System in Java ensuring complete clinical equipment safety.
* **Specific Functional Objectives:**
  1. **Automated Tolerance Verification:** Implement precision measurement validation using absolute deviation formulas.
  2. **Automated Safety Lockout:** Automatically lock compromised equipment to prevent medical staff from scheduling procedures on uncertified hardware.
  3. **Strict Maintenance State Machine:** Enforce work order progression (`REQUESTED` → `SCHEDULED` → `IN_PROGRESS` → `COMPLETED`) with safety gates.
  4. **Cryptographic Role-Based Access Control (RBAC):** Restrict system privileges (`ADMIN`, `TECHNICIAN`, `STAFF`, `AUDITOR`) with salted **SHA-256** password hashing.
  5. **Dynamic Compliance Telemetry:** Formulate real-time Key Performance Indicators (Fleet Availability, Compliance %, Mean Downtime).
  6. **Crash-Resilient Persistence:** Guarantee data integrity via atomic temporary file swapping without requiring third-party database engines.

---

## SLIDE 5: EXISTING SYSTEM VS. PROPOSED SYSTEM

### [Slide Title]
**EXISTING SYSTEM VS. PROPOSED SYSTEM**

### [Slide Content]

| Parameter | Existing System (Manual / Basic Spreadsheets) | Proposed System (Biomedical Java System) |
|---|---|---|
| **Data Recording** | Paper logbooks, physical index cards, Excel files | Atomic-swap RFC-4180 CSV structured persistence |
| **Tolerance Checking** | Manual mental calculation by technicians | Programmatic check: $|\text{Measured} - \text{Nominal}| \le \text{Tolerance}$ |
| **Failed Test Outcome** | Device physically tagged with delay; risk of usage | Instant automated software lock to `OUT_OF_SERVICE` |
| **Post-Repair Procedure** | Device immediately restored to patient care | Forced transition to `CALIBRATION_PENDING` |
| **Access Security** | Open access; unauthorized edits possible | Salted SHA-256 encryption; 4-tier RBAC protection |
| **Audit Compliance** | Fragmented papers; difficult during inspections | Automated timestamped, tamper-evident audit logs |
| **User Interfaces** | Disconnected data entry forms | Dual UI: ANSI Terminal CLI + Embedded Web Dashboard |
| **KPI Visibility** | End-of-month manual statistical reporting | Real-time visual meters for fleet compliance % |

---

## SLIDE 6: LITERATURE SURVEY

### [Slide Title]
**LITERATURE SURVEY**

### [Slide Content]
* **Study 1: Quality Management and Calibration in Healthcare (ISO 17025 / ISO 13485 Standards)**
  * *Findings:* Medical equipment errors account for a measurable percentage of adverse clinical events. Regular calibration prevents drift in critical sensors (e.g., airway pressure, flow sensors).
  * *Identified Gap:* Commercial solutions focus heavily on asset accounting and billing rather than real-time tolerance safety interlocks.
* **Study 2: Computerized Maintenance Management Systems (CMMS) in Hospital Engineering**
  * *Findings:* Structured preventive maintenance schedules reduce mean downtime by up to 40% and double equipment operational life.
  * *Identified Gap:* High licensing costs and database infrastructure dependencies hinder deployment in decentralized triage and mobile clinical camps.
* **Study 3: Role-Based Access Control (RBAC) & Audit Integrity in Clinical Systems**
  * *Findings:* Clinical systems require clear segregation of duties between nurses reporting defects and biomedical engineers executing repairs to prevent fraudulent compliance records.
  * *Application in Project:* Guided the separation of the 4 user roles (`ADMIN`, `TECHNICIAN`, `STAFF`, `AUDITOR`) and salted SHA-256 hashing.

---

## SLIDE 7: SYSTEM ARCHITECTURE

### [Slide Title]
**SYSTEM ARCHITECTURE & DESIGN**

### [Slide Content]
* **Layered Clean Architecture Pattern:**
  * **1. Presentation Layer:**
    * Interactive Terminal Console (`Main.java`) with formatted ANSI tables.
    * Embedded HTTP Web Server (`com.sun.net.httpserver`) serving an interactive Single Page Application (`web/index.html`).
  * **2. Service (Business Logic) Layer:**
    * `DeviceService`: Enforces device lifecycle and risk calculation.
    * `CalibrationService`: Manages tolerance checks, due-dates, and digital certificates.
    * `MaintenanceService`: Manages work orders and post-repair interlocks.
    * `ComplianceService`: Aggregates hospital-wide KPI metrics.
    * `AuthService`: Validates credentials using cryptographic hashing.
  * **3. Repository Layer:**
    * `DeviceRepository`, `CalibrationRepository`, `MaintenanceRepository`, `UserRepository`, `AuditRepository`.
  * **4. Model Layer:**
    * Object hierarchies with domain invariants (`LifeSupportDevice`, `DiagnosticDevice`, `LaboratoryDevice`).
  * **5. Persistence Layer:**
    * Atomic temp-file swapping for RFC-4180 CSV files under `data/`.

---

## SLIDE 8: METHODOLOGY

### [Slide Title]
**METHODOLOGY & DESIGN PRINCIPLES**

### [Slide Content]
* **1. Object-Oriented Analysis & Design (OOAD):**
  * **Encapsulation:** Private entity fields with strict domain validation (rejection of negative tolerances, negative downtime, blank usernames).
  * **Inheritance:** Multi-tier class hierarchies (`BaseEntity` → `BiomedicalDevice` → `LifeSupportDevice`).
  * **Polymorphism:** Method overriding for device-specific risk scores (`calculateRiskScore()`) and pre-operational checks (`passesPreCheck()`).
  * **Abstraction:** Contract-driven interfaces (`Calibratable`, `Maintainable`, `Auditable`, `Reportable`).
* **2. Finite State Machine for Maintenance:**
  * Strict legal status transitions: `REQUESTED` → `SCHEDULED` → `IN_PROGRESS` → `COMPLETED`.
* **3. Fail-Safe Verification Engine:**
  * Formulated mathematical models for parameter deviation checks.
* **4. Test-Driven Verification:**
  * Automated testing combining JUnit 5 and a custom zero-dependency `StandaloneTestRunner` executing 14 unit test assertions.

---

## SLIDE 9: IMPLEMENTATION AND MODULES

### [Slide Title]
**IMPLEMENTATION & MODULE BREAKDOWN**

### [Slide Content]
* **Module 1: Biomedical Device Lifecycle Management**
  * Device registration with serial tracking, department mapping, and risk evaluation.
  * Permanent decommission locks preventing retired equipment from accidental reactivation.
* **Module 2: Dynamic Calibration & Certification Engine**
  * Mathematical evaluation:
    $$\text{Deviation} = |\text{Measured Value} - \text{Nominal Value}|$$
    $$\text{Status} = \begin{cases} \text{PASS}, & \text{if Deviation} \le \text{Allowed Tolerance} \\ \text{FAIL}, & \text{if Deviation} > \text{Allowed Tolerance} \end{cases}$$
  * Generates digital certificates (`CERT-YYYY-XXXX`) and shifts calibration interval (`+180 days`).
* **Module 3: Maintenance Operations & Safety Interlock**
  * Work orders for Preventive, Corrective, and Emergency repairs.
  * **Safety Interlock:** Replaced sensors force device into `CALIBRATION_PENDING`.
* **Module 4: Real-Time Compliance Dashboard & Reporting**
  * Dynamic formula for hospital compliance:
    $$\text{Compliance } \% = \frac{\text{Valid} + \text{Due Soon}}{\text{Total Devices Requiring Calibration}} \times 100$$
  * Automated CSV and TXT report generator (`reports/` directory).
* **Module 5: Security, RBAC & Audit Trails**
  * Salted SHA-256 password hashing. Chronological audit logging of all clinical actions.

---

## SLIDE 10: RESULTS AND SCREENSHOTS

### [Slide Title]
**RESULTS AND SYSTEM DEMONSTRATION SCREENSHOTS**

### [Slide Content]
* **Result 1: Real-Time Clinical Dashboard (Web SPA & Console)**
  * Visual KPI tiles displaying Fleet Compliance Rate, Active Inventory, Out-of-Service devices, and Pending Maintenance Work Orders.
* **Result 2: Successful Calibration & Digital Certificate Generation**
  * Verified Mechanical Ventilator (`DEV-1001`): Nominal $500.0\text{ mL}$, Measured $502.1\text{ mL}$, Tolerance $\pm 15.0\text{ mL}$.
  * Result: Test PASSED $\rightarrow$ Issued formal certificate `CERT-2026-XXXX` $\rightarrow$ Updated calibration due date.
* **Result 3: Automated Fail-Safe Safety Lockout**
  * Tested Patient Monitor (`DEV-1003`): Nominal $1.0\text{ mV}$, Measured $1.45\text{ mV}$, Tolerance $\pm 0.05\text{ mV}$.
  * Result: Test FAILED (Deviation $0.45 > 0.05$) $\rightarrow$ Immediate lock to `OUT_OF_SERVICE`.
* **Result 4: Maintenance Recertification Interlock**
  * Completed motherboard replacement on `DEV-1003`. Device automatically transitioned to `CALIBRATION_PENDING` (barred from clinical deployment until re-certified).
* **Result 5: Automated Test Suite Execution**
  * Zero-dependency test runner passing 14/14 unit tests and the End-to-End hospital shift verification test.

---

## SLIDE 11: CONCLUSION AND FUTURE ENHANCEMENT

### [Slide Title]
**CONCLUSION AND FUTURE ENHANCEMENTS**

### [Slide Content]
#### Conclusion:
* Successfully engineered a reliable, enterprise-grade Biomedical Device Calibration and Maintenance Management System in Java.
* Demonstrated how Core OOP principles directly enforce clinical engineering safety and hospital regulatory compliance.
* Prevented medical hazards through automated tolerance checking, fail-safe lockouts, and post-repair recalibration interlocks.
* Delivered a lightweight, zero-dependency deployment with atomic persistence and dual console/web interfaces.

#### Future Enhancements:
* **IoT Telemetry Integration:** Ingest live physical sensor readings via MQTT/CoAP for real-time drift detection.
* **AI Predictive Maintenance:** Train machine learning models (Random Forest / Regression) on historical downtime to predict component failure before clinical breakdowns.
* **HL7 / FHIR Standards:** Integrate with hospital Electronic Health Record (EHR) systems to cross-verify device status with patient bedside monitors.
* **Mobile Technician Client:** Barcode/QR-code scanning on Android/iOS for rapid bedside device check-ins.

---

## SLIDE 12: REFERENCES

### [Slide Title]
**REFERENCES**

### [Slide Content]
1. **IEEE Transactions on Biomedical Engineering:** *"Lifecycle Governance and Risk-Based Inspection of Medical Equipment in Tertiary Healthcare Facilities."*
2. **ISO/IEC 17025:2017:** *"General Requirements for the Competence of Testing and Calibration Laboratories,"* International Organization for Standardization.
3. **ISO 13485:2016:** *"Medical Devices — Quality Management Systems — Requirements for Regulatory Purposes."*
4. **Joshua Bloch:** *"Effective Java,"* 3rd Edition, Addison-Wesley Professional, 2018.
5. **World Health Organization (WHO):** *"Medical Equipment Maintenance Programme Overview,"* WHO Medical Device Technical Series, Geneva.
6. **Erickson, J. & Siau, K.:** *"Object-Oriented Analysis and Design in Medical Information Systems,"* Journal of Computer Information Systems.

---

## SLIDE 13: LIVE DEMO / PROJECT DEMONSTRATION

### [Slide Title]
**LIVE DEMO / PROJECT DEMONSTRATION**

### [Slide Content]
* **Demo Scenario 1: Pass Workflow & Certificate Issuance**
  * Login as Technician (`tech1`) $\rightarrow$ Select `DEV-1001` (Mechanical Ventilator).
  * Input Tidal Volume: Nominal $500\text{ mL}$, Measured $502.1\text{ mL}$ $\rightarrow$ **PASS**.
  * View issued digital certificate and +180 days due-date extension.
* **Demo Scenario 2: Fail-Safe Safety Lockout**
  * Select `DEV-1003` (Patient Monitor).
  * Input ECG Amplitude: Nominal $1.0\text{ mV}$, Measured $1.45\text{ mV}$, Tolerance $0.05\text{ mV}$ $\rightarrow$ **FAIL**.
  * Observe immediate safety lock to `OUT_OF_SERVICE`.
* **Demo Scenario 3: Maintenance Workflow & Safety Interlock**
  * Staff files repair request $\rightarrow$ Admin schedules repair $\rightarrow$ Technician logs replacement of ECG Motherboard.
  * Observe device transition to `CALIBRATION_PENDING` (cannot be used until re-tested).
* **Demo Scenario 4: Live HTTP Web Dashboard**
  * Open `http://localhost:8088/` $\rightarrow$ Real-time compliance meters, visual work order Kanban, and one-click report exports.
