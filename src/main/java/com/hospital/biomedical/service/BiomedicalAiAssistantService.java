package com.hospital.biomedical.service;

import com.hospital.biomedical.model.device.BiomedicalDevice;
import com.hospital.biomedical.model.enums.CalibrationDueStatus;
import com.hospital.biomedical.model.enums.DeviceStatus;
import com.hospital.biomedical.model.user.User;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent AI Copilot & Clinical Advisory Service for Biomedical Governance.
 * Supports both cloud Generative AI (Google Gemini) and a deterministic
 * Clinical Biomedical Expert Engine for guaranteed zero-downtime assistance.
 */
public class BiomedicalAiAssistantService {
    private final DeviceService deviceService;
    private final ComplianceService complianceService;
    private final CalibrationService calibrationService;
    private final MaintenanceService maintenanceService;

    private final String geminiApiKey;
    private final HttpClient httpClient;

    public record AiResponse(
            String reply,
            String engine,
            List<String> suggestions
    ) {}

    public BiomedicalAiAssistantService(DeviceService deviceService,
                                       ComplianceService complianceService,
                                       CalibrationService calibrationService,
                                       MaintenanceService maintenanceService) {
        this.deviceService = deviceService;
        this.complianceService = complianceService;
        this.calibrationService = calibrationService;
        this.maintenanceService = maintenanceService;

        String key = System.getenv("GEMINI_API_KEY");
        this.geminiApiKey = (key != null) ? key.trim() : "";

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
    }

    /**
     * Main chat entrypoint. Attempts cloud Gemini AI first; falls back to Clinical Expert Engine.
     */
    public AiResponse askAi(String userPrompt, User currentUser) {
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            return new AiResponse(
                    "Hello! I am your **BioMed AI Copilot**. How can I assist you with clinical equipment, calibration verification, or compliance audits today?",
                    "BioMed Clinical Intelligence",
                    getDefaultSuggestions()
            );
        }

        String prompt = userPrompt.trim();

        // 1. Try Cloud Gemini API
        String cloudAnswer = tryGeminiApi(prompt, currentUser);
        if (cloudAnswer != null && !cloudAnswer.isBlank()) {
            return new AiResponse(cloudAnswer, "Gemini AI (Cloud Enhanced)", getContextualSuggestions(prompt));
        }

        // 2. Deterministic Clinical Expert Knowledge Engine
        String localAnswer = generateExpertEngineResponse(prompt, currentUser);
        return new AiResponse(localAnswer, "BioMed Clinical Expert System (Offline Ready)", getContextualSuggestions(prompt));
    }

    /**
     * Calls Gemini Generative Language API with hospital telemetry grounding.
     */
    private String tryGeminiApi(String userPrompt, User currentUser) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return null;
        }

        String[] candidateModels = {"gemini-2.5-flash", "gemini-flash-latest", "gemini-3.8-flash"};

        // Build grounded system telemetry
        ComplianceService.ComplianceDashboardData metrics = complianceService.getDashboardMetrics();
        int totalDevices = metrics.totalDevices();
        double complianceRate = metrics.calibrationCompliancePercentage();
        int overdueCount = metrics.overdueCalibrations();
        String operatorRole = currentUser != null ? currentUser.getRole().name() : "GUEST";
        String operatorName = currentUser != null ? currentUser.getFullName() : "Operator";

        String systemContext = String.format(
                "You are BioMed AI Copilot, a certified clinical biomedical engineering assistant for a hospital equipment management system.\\n" +
                "Live Telemetry Context:\\n" +
                "- Operator: %s (Role: %s)\\n" +
                "- Total Hospital Devices: %d\\n" +
                "- Current Compliance Rate: %.1f%%\\n" +
                "- Overdue Calibration Count: %d\\n\\n" +
                "Rules:\\n" +
                "1. Always enforce patient safety. Deviation = |Measured - Nominal|. If Deviation > Tolerance, device MUST be OUT_OF_SERVICE.\\n" +
                "2. When maintenance repair completes, equipment is CALIBRATION_PENDING until post-repair calibration passes.\\n" +
                "3. Keep answers concise, professional, and formatted in clean Markdown with bullet points.\\n" +
                "Operator Query: %s",
                operatorName, operatorRole, totalDevices, complianceRate, overdueCount, escapeJson(userPrompt)
        );

        String jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":\"" + systemContext + "\"}]}]}";

        for (String model : candidateModels) {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + geminiApiKey;
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(5))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    String extracted = extractGeminiCandidateText(response.body());
                    if (extracted != null && !extracted.isBlank()) {
                        return extracted;
                    }
                }
            } catch (Exception ignored) {
                // Graceful fallback to next model or expert engine
            }
        }
        return null;
    }

    /**
     * Extracts text from Gemini JSON response without external JSON libraries.
     */
    private String extractGeminiCandidateText(String json) {
        try {
            int textIdx = json.indexOf("\"text\": \"");
            if (textIdx != -1) {
                int start = textIdx + 9;
                int end = json.indexOf("\"", start);
                while (end != -1 && json.charAt(end - 1) == '\\') {
                    end = json.indexOf("\"", end + 1);
                }
                if (end > start) {
                    String raw = json.substring(start, end);
                    return unescapeJson(raw);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Comprehensive deterministic biomedical advisory knowledge base.
     */
    public String generateExpertEngineResponse(String prompt, User currentUser) {
        String lower = prompt.toLowerCase(Locale.ROOT);
        LocalDate today = LocalDate.now();

        // 1. Specific Device Lookup (e.g. "DEV-1001", "DEV-2001", "ventilator")
        Pattern devPattern = Pattern.compile("(dev-[0-9a-zA-Z-]+)", Pattern.CASE_INSENSITIVE);
        Matcher devMatcher = devPattern.matcher(prompt);
        if (devMatcher.find()) {
            String devId = devMatcher.group(1).toUpperCase(Locale.ROOT);
            try {
                BiomedicalDevice d = deviceService.getDeviceById(devId);
                return String.format(
                        "### 🔍 Telemetry Dossier: **%s** (%s)\n\n" +
                        "- **Device Model:** %s\n" +
                        "- **Department:** %s\n" +
                        "- **Operational Status:** `%s`\n" +
                        "- **Calibration Due Status:** `%s` (Interval: every %d days)\n" +
                        "- **Next Calibration Date:** `%s`\n" +
                        "- **Asset Tag / Serial:** `%s` / `%s`\n" +
                        "- **Calculated Risk Score:** `%.2f`\n\n" +
                        "💡 *Recommended Action:* %s",
                        d.getId(), d.getName(),
                        d.getModelNumber(),
                        d.getDepartment(),
                        d.getDeviceStatus(),
                        d.getCalibrationDueStatus(today), d.getCalibrationFrequencyDays(),
                        d.getNextCalibrationDate() != null ? d.getNextCalibrationDate().toString() : "Not Scheduled",
                        d.getAssetNumber(), d.getSerialNumber(),
                        d.calculateRiskScore(),
                        getRecommendedDeviceAction(d)
                );
            } catch (Exception ignored) {
                return "Device `" + devId + "` was not found in the hospital inventory. Please check the Asset Number or Device ID in the inventory table.";
            }
        }

        // 2. Overdue Devices & Immediate Attention
        if (lower.contains("overdue") || lower.contains("expired") || lower.contains("attention")) {
            List<BiomedicalDevice> devices = deviceService.findDevices();
            List<BiomedicalDevice> overdue = devices.stream()
                    .filter(d -> d.getCalibrationDueStatus(today) == CalibrationDueStatus.OVERDUE)
                    .toList();
            List<BiomedicalDevice> attention = devices.stream()
                    .filter(d -> d.getDeviceStatus() == DeviceStatus.OUT_OF_SERVICE
                            || d.getDeviceStatus() == DeviceStatus.CALIBRATION_PENDING
                            || d.getCalibrationDueStatus(today) == CalibrationDueStatus.OVERDUE)
                    .toList();

            StringBuilder sb = new StringBuilder();
            sb.append("### ⚠️ Equipment Requiring Urgent Clinical Action\n\n");
            sb.append(String.format("- **Total Overdue Devices:** `%d`\n", overdue.size()));
            sb.append(String.format("- **Immediate Attention Queue:** `%d`\n\n", attention.size()));

            if (attention.isEmpty()) {
                sb.append("✅ **All devices are currently compliant and safe for patient use!**\n");
            } else {
                sb.append("| Device ID | Name | Department | Status | Due Date | Risk |\n");
                sb.append("| :--- | :--- | :--- | :--- | :--- | :--- |\n");
                for (BiomedicalDevice d : attention.stream().limit(6).toList()) {
                    sb.append(String.format("| `%s` | %s | %s | `%s` | `%s` | `%.1f` |\n",
                            d.getId(), d.getName(), d.getDepartment(),
                            d.getDeviceStatus(), d.getNextCalibrationDate(), d.calculateRiskScore()));
                }
                sb.append("\n> **Safety Directive:** Devices marked `OUT_OF_SERVICE` or `OVERDUE` must be sequestered immediately. Do not attach to patients until recertified.\n");
            }
            return sb.toString();
        }

        // 3. Hospital Compliance & KPI Health Check
        if (lower.contains("compliance") || lower.contains("kpi") || lower.contains("health") || lower.contains("rate")) {
            ComplianceService.ComplianceDashboardData data = complianceService.getDashboardMetrics(today);
            double rate = data.calibrationCompliancePercentage();
            int total = data.totalDevices();
            int active = data.activeDevices();
            int overdue = data.overdueCalibrations();

            String statusLevel = rate >= 90.0 ? "🟢 OPTIMAL (Regulatory Safe)" : (rate >= 75.0 ? "🟡 WARNING (Audit Risk)" : "🔴 CRITICAL (Non-Compliant)");

            return String.format(
                    "### 📊 Hospital Compliance & Safety Audit Status\n\n" +
                    "- **Hospital Compliance Rate:** `%.1f%%` — **%s**\n" +
                    "- **Total Medical Equipment:** `%d`\n" +
                    "- **Currently Operational:** `%d`\n" +
                    "- **Overdue Devices:** `%d`\n\n" +
                    "**Compliance Standard Formula:**\n" +
                    "$$\\text{Compliance Rate} = \\left(\\frac{\\text{Compliant Non-Exempt Devices}}{\\text{Total Active Non-Exempt Devices}}\\right) \\times 100$$\n\n" +
                    "📁 *Export:* Click the **Export Reports** button in the top header to download the official **Hospital Compliance Audit Summary (.txt)** for Joint Commission or FDA inspections.",
                    rate, statusLevel, total, active, overdue
            );
        }

        // 4. Calibration Tolerance & Formulas
        if (lower.contains("tolerance") || lower.contains("formula") || lower.contains("calculate deviation") || lower.contains("math") || lower.contains("deviation")) {
            return """
                    ### 📐 Calibration Tolerance & Mathematical Verification Rules

                    In accordance with **IEC 60601** and clinical engineering standards, every test parameter is mathematically evaluated:

                    1. **Deviation Calculation:**
                       $$\\text{Deviation} = |\\text{Measured Value} - \\text{Nominal (Reference) Value}|$$

                    2. **Acceptance Criteria:**
                       $$\\text{Deviation} \\le \\text{Allowed Tolerance} \\implies \\mathbf{[PASS]}$$
                       $$\\text{Deviation} > \\text{Allowed Tolerance} \\implies \\mathbf{[FAIL]}$$

                    3. **Standard Reference Limits:**
                       - **Defibrillator:** Nominal $200.0\\text{ J}$, Allowed Tolerance $\\pm 4.0\\text{ J}$ ($\\pm 2\\%$)
                       - **Infusion Pump:** Nominal $100.0\\text{ mL/hr}$, Allowed Tolerance $\\pm 2.0\\text{ mL/hr}$
                       - **ECG Monitor (Lead II):** Nominal $1.000\\text{ mV}$, Allowed Tolerance $\\pm 0.050\\text{ mV}$
                       - **Mechanical Ventilator:** Nominal $500.0\\text{ mL}$, Allowed Tolerance $\\pm 25.0\\text{ mL}$
                       - **Centrifuge:** Nominal $4000.0\\text{ RPM}$, Allowed Tolerance $\\pm 50.0\\text{ RPM}$

                    ⚠️ **Safety Lockout:** If even **one** measurement fails tolerance, the test fails, and the system **immediately locks the device to `OUT_OF_SERVICE`**.
                    """;
        }

        // 5. Post-Maintenance Recalibration Safety Interlock
        if (lower.contains("interlock") || lower.contains("post-maintenance") || lower.contains("recalibration") || lower.contains("repair")) {
            return """
                    ### 🔒 Post-Maintenance Safety Recalibration Interlock

                    **Why is a device not immediately `OPERATIONAL` after a repair?**

                    - **Clinical Danger:** When a technician replaces parts (e.g. motors, boards, flow sensors), calibration characteristics shift. Returning a device directly to patient care without verification causes catastrophic clinical errors.
                    - **System Enforced Workflow:**
                      1. Maintenance status transitions: `REQUESTED` $\\rightarrow$ `IN_PROGRESS` $\\rightarrow$ `COMPLETED`.
                      2. Upon completion, the system transitions device status to **`CALIBRATION_PENDING`** (NOT `OPERATIONAL`).
                      3. Operational clinical use is strictly prohibited.
                      4. A certified technician must run a formal **Post-Maintenance Calibration Test**.
                      5. Only when the calibration verifies within tolerances does the device safely return to **`OPERATIONAL`**.
                    """;
        }

        // 6. Regulatory Standards (FDA, Joint Commission, NABH, IEC)
        if (lower.contains("fda") || lower.contains("regulation") || lower.contains("standard") || lower.contains("audit") || lower.contains("tjc")) {
            return """
                    ### 📜 Medical Device Regulatory Governance Framework

                    This management system enforces compliance across three premier global standards:

                    - **FDA 21 CFR Part 820 (Quality System Regulation):**
                      Mandates documented inspection, calibration verification, maintenance logs, and electronic record authentication.
                    - **The Joint Commission (TJC) Standard EC.02.04.01:**
                      Hospitals must maintain a 100% scheduled maintenance record on life-support equipment and minimum 95% on non-life-support assets.
                    - **IEC 60601-1 (Medical Electrical Equipment Safety):**
                      Enforces strict physical and electrical safety thresholds, leakage testing, and grounding verification.
                    - **Tamper-Evident Audit Trail:**
                      All actions in this system record actor IDs, timestamps, and SHA-hashed logs to prevent retrospective modification.
                    """;
        }

        // 7. General Help & System Capabilities
        return """
                ### 💡 BioMed AI Copilot — Clinical Operations Guide

                I can help you operate the system and comply with biomedical safety protocols:

                - **Device Diagnostics:** Inquire about any device (e.g., `Tell me about DEV-1001` or `Status of ICU Ventilator`).
                - **Overdue Audits:** Ask `Show overdue equipment` or `Which devices need attention?`.
                - **Compliance Telemetry:** Ask `What is our current compliance rate?`.
                - **Calibration Math:** Ask `How is deviation calculated?` or `Defibrillator tolerances`.
                - **Safety Workflows:** Ask `Explain the post-maintenance safety interlock`.
                - **Certified Reports:** Click **Export Reports** in the header to download CSV/TXT audit dossiers.

                Select one of the quick suggestions below or type your clinical query!
                """;
    }

    private String getRecommendedDeviceAction(BiomedicalDevice d) {
        if (d.getDeviceStatus() == DeviceStatus.OUT_OF_SERVICE) {
            return "Device is currently OUT OF SERVICE. Create a maintenance work order or inspect calibration failures.";
        } else if (d.getDeviceStatus() == DeviceStatus.CALIBRATION_PENDING) {
            return "Device is awaiting mandatory post-repair calibration. Run a calibration test before placing back in service.";
        } else if (d.getDeviceStatus() == DeviceStatus.UNDER_MAINTENANCE) {
            return "Device is under active repair. Complete the work order once parts and electrical checks are verified.";
        } else if (d.getNextCalibrationDate() != null && d.getNextCalibrationDate().isBefore(LocalDate.now())) {
            return "Calibration is OVERDUE! Schedule a certified recalibration immediately.";
        }
        return "Device is operational, certified, and compliant.";
    }

    public List<String> getDefaultSuggestions() {
        return List.of(
                "📊 Hospital Compliance Health Check",
                "⚠️ Show Overdue Equipment",
                "📐 Calibration Tolerance Formulas",
                "🔒 Post-Maintenance Safety Interlock",
                "🔍 Tell me about DEV-1001",
                "📜 Regulatory Standards (FDA & TJC)"
        );
    }

    public List<String> getContextualSuggestions(String query) {
        String lower = query.toLowerCase(Locale.ROOT);
        if (lower.contains("dev-")) {
            return List.of(
                    "Run calibration on this device",
                    "Create maintenance work order",
                    "Check overdue equipment roster",
                    "📊 Compliance Health Check"
            );
        } else if (lower.contains("overdue") || lower.contains("attention")) {
            return List.of(
                    "Export Overdue Calibrations CSV",
                    "Explain safety lockout rules",
                    "Tell me about DEV-1001",
                    "📊 Compliance Health Check"
            );
        }
        return getDefaultSuggestions();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String unescapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
