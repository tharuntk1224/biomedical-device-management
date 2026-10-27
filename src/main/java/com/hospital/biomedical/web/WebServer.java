package com.hospital.biomedical.web;

import com.hospital.biomedical.exception.DeviceNotFoundException;
import com.hospital.biomedical.model.calibration.CalibrationMeasurement;
import com.hospital.biomedical.model.calibration.CalibrationRecord;
import com.hospital.biomedical.model.device.BiomedicalDevice;
import com.hospital.biomedical.model.device.DiagnosticDevice;
import com.hospital.biomedical.model.device.LaboratoryDevice;
import com.hospital.biomedical.model.device.LifeSupportDevice;
import com.hospital.biomedical.model.enums.*;
import com.hospital.biomedical.model.maintenance.AbstractMaintenanceRecord;
import com.hospital.biomedical.model.maintenance.MaintenanceRequest;
import com.hospital.biomedical.model.user.User;
import com.hospital.biomedical.service.*;
import com.hospital.biomedical.util.DateUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/**
 * Embedded HTTP Web Server providing REST APIs and modern web dashboard.
 * Zero external dependencies (uses standard Java com.sun.net.httpserver).
 */
public class WebServer {
    private final DeviceService deviceService;
    private final CalibrationService calibrationService;
    private final MaintenanceService maintenanceService;
    private final ComplianceService complianceService;
    private final AuthService authService;
    private final AuditService auditService;
    private final ReportService reportService;
    private final BiomedicalAiAssistantService aiAssistantService;
    private HttpServer server;
    private int port = 8080;

    public WebServer(DeviceService deviceService, CalibrationService calibrationService,
                     MaintenanceService maintenanceService, ComplianceService complianceService,
                     AuthService authService, AuditService auditService, ReportService reportService) {
        this(deviceService, calibrationService, maintenanceService, complianceService, authService, auditService, reportService,
                new BiomedicalAiAssistantService(deviceService, complianceService, calibrationService, maintenanceService));
    }

    public WebServer(DeviceService deviceService, CalibrationService calibrationService,
                     MaintenanceService maintenanceService, ComplianceService complianceService,
                     AuthService authService, AuditService auditService, ReportService reportService,
                     BiomedicalAiAssistantService aiAssistantService) {
        this.deviceService = deviceService;
        this.calibrationService = calibrationService;
        this.maintenanceService = maintenanceService;
        this.complianceService = complianceService;
        this.authService = authService;
        this.auditService = auditService;
        this.reportService = reportService;
        this.aiAssistantService = aiAssistantService;
    }

    public synchronized int start() throws IOException {
        int[] candidatePorts = {8080, 8088, 8888, 9090, 8000};
        for (int p : candidatePorts) {
            try {
                server = HttpServer.create(new InetSocketAddress(p), 0);
                this.port = p;
                break;
            } catch (IOException ignored) {}
        }

        if (server == null) {
            server = HttpServer.create(new InetSocketAddress(0), 0);
            this.port = server.getAddress().getPort();
        }

        // Context Handlers
        server.createContext("/", new StaticAppHandler());
        server.createContext("/api/compliance", new ComplianceApiHandler());
        server.createContext("/api/devices", new DevicesApiHandler());
        server.createContext("/api/calibrations", new CalibrationsApiHandler());
        server.createContext("/api/maintenance", new MaintenanceApiHandler());
        server.createContext("/api/audit", new AuditApiHandler());
        server.createContext("/api/auth", new AuthApiHandler());
        server.createContext("/api/reports", new ReportsApiHandler());
        server.createContext("/api/ai", new AiAssistantApiHandler());

        server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        return this.port;
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public int getPort() {
        return port;
    }

    public String getUrl() {
        return "http://localhost:" + port + "/";
    }

    // Helper to send JSON responses
    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private static Map<String, String> parseFormOrJsonParams(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.trim().isEmpty()) return map;

        body = body.trim();
        if (body.startsWith("{") && body.endsWith("}")) {
            // Simple JSON key-value extraction
            String content = body.substring(1, body.length() - 1);
            String[] pairs = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            for (String pair : pairs) {
                String[] kv = pair.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 2);
                if (kv.length == 2) {
                    String k = kv[0].trim().replace("\"", "");
                    String v = kv[1].trim().replace("\"", "");
                    map.put(k, v);
                }
            }
        } else {
            // URL Encoded form
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                    String v = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    map.put(k, v);
                }
            }
        }
        return map;
    }

    // ==========================================
    // 1. STATIC APP HANDLER (HTML5 SPA)
    // ==========================================
    private class StaticAppHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/api/")) {
                sendJsonResponse(exchange, 404, "{\"error\":\"Endpoint not found\"}");
                return;
            }

            String html = WebFrontend.getHtmlPage();
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // ==========================================
    // 2. COMPLIANCE API HANDLER
    // ==========================================
    private class ComplianceApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            ComplianceService.ComplianceDashboardData kpis = complianceService.getDashboardMetrics();
            String json = String.format(Locale.US,
                    "{\"totalDevices\":%d,\"activeDevices\":%d,\"outOfService\":%d,\"underMaintenance\":%d," +
                    "\"validCalibrations\":%d,\"dueSoon\":%d,\"dueToday\":%d,\"overdue\":%d,\"exempt\":%d," +
                    "\"failedCalibrations\":%d,\"openRequests\":%d,\"completedJobs\":%d,\"totalCost\":%.2f," +
                    "\"totalDowntime\":%.1f,\"compliancePercentage\":%.1f,\"compliantCount\":%d,\"subjectCount\":%d}",
                    kpis.totalDevices(), kpis.activeDevices(), kpis.outOfServiceDevices(), kpis.underMaintenanceDevices(),
                    kpis.validCalibrations(), kpis.dueSoonCalibrations(), kpis.dueTodayCalibrations(),
                    kpis.overdueCalibrations(), kpis.notRequiredCalibrations(), kpis.failedCalibrations(),
                    kpis.openMaintenanceRequests(), kpis.completedMaintenanceJobs(),
                    kpis.totalMaintenanceCost().doubleValue(), kpis.totalDowntimeHours(),
                    kpis.calibrationCompliancePercentage(), kpis.compliantDevicesCount(), kpis.subjectDevicesCount());
            sendJsonResponse(exchange, 200, json);
        }
    }

    // ==========================================
    // 3. DEVICES API HANDLER
    // ==========================================
    private class DevicesApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equalsIgnoreCase(method)) {
                List<BiomedicalDevice> devices = deviceService.findDevices();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < devices.size(); i++) {
                    BiomedicalDevice d = devices.get(i);
                    if (i > 0) sb.append(",");
                    sb.append(String.format(Locale.US,
                            "{\"id\":\"%s\",\"assetNumber\":\"%s\",\"name\":\"%s\",\"manufacturer\":\"%s\"," +
                            "\"model\":\"%s\",\"department\":\"%s\",\"category\":\"%s\",\"status\":\"%s\"," +
                            "\"riskLevel\":\"%s\",\"riskScore\":%.2f,\"preCheck\":%b,\"calibRequired\":%b," +
                            "\"frequency\":%d,\"lastCalib\":\"%s\",\"nextCalib\":\"%s\",\"dueStatus\":\"%s\"," +
                            "\"tech\":\"%s\"}",
                            d.getId(), d.getAssetNumber(), escapeJson(d.getName()), escapeJson(d.getManufacturer()),
                            escapeJson(d.getModelNumber()), escapeJson(d.getDepartment()), d.getCategory().name(),
                            d.getDeviceStatus().name(), d.getRiskLevel().name(), d.calculateRiskScore(),
                            d.passesPreCheck(), d.isCalibrationRequired(), d.getCalibrationFrequencyDays(),
                            DateUtils.formatDate(d.getLastCalibrationDate()), DateUtils.formatDate(d.getNextCalibrationDate()),
                            d.getCalibrationDueStatus(LocalDate.now()).name(), d.getAssignedTechnicianId()));
                }
                sb.append("]");
                sendJsonResponse(exchange, 200, sb.toString());

            } else if ("POST".equalsIgnoreCase(method)) {
                String body = readRequestBody(exchange);
                Map<String, String> p = parseFormOrJsonParams(body);

                if (path.endsWith("/retire")) {
                    String id = p.get("id");
                    String reason = p.getOrDefault("reason", "Decommissioned via Web Console");
                    try {
                        authService.login("admin", "Admin@123"); // Elevated admin session for web action
                        BiomedicalDevice d = deviceService.retireDevice(id, reason);
                        sendJsonResponse(exchange, 200, "{\"success\":true,\"status\":\"RETIRED\"}");
                    } catch (Exception e) {
                        sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
                    }
                    return;
                }

                // Register Device
                try {
                    authService.login("admin", "Admin@123");
                    String type = p.getOrDefault("type", "1");
                    String asset = p.get("assetNumber");
                    String serial = p.get("serialNumber");
                    String name = p.get("name");
                    String mfr = p.get("manufacturer");
                    String model = p.get("model");
                    String dept = p.get("department");
                    int freq = Integer.parseInt(p.getOrDefault("frequency", "180"));

                    BiomedicalDevice device;
                    if ("1".equals(type) || "LIFE_SUPPORT".equalsIgnoreCase(type)) {
                        int bat = Integer.parseInt(p.getOrDefault("battery", "120"));
                        boolean alarms = Boolean.parseBoolean(p.getOrDefault("alarms", "true"));
                        device = new LifeSupportDevice(null, asset, serial, name, mfr, model, dept, freq, bat, alarms);
                    } else if ("2".equals(type) || "DIAGNOSTIC".equalsIgnoreCase(type)) {
                        boolean rad = Boolean.parseBoolean(p.getOrDefault("radiation", "false"));
                        device = new DiagnosticDevice(null, asset, serial, name, mfr, model, dept, RiskLevel.MEDIUM, freq, rad, "v1.0");
                    } else {
                        boolean temp = Boolean.parseBoolean(p.getOrDefault("tempCtrl", "false"));
                        double tempC = Double.parseDouble(p.getOrDefault("tempCelsius", "22.0"));
                        device = new LaboratoryDevice(null, asset, serial, name, mfr, model, dept, RiskLevel.LOW, freq, temp, tempC, 1);
                    }

                    deviceService.registerDevice(device);
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"id\":\"" + device.getId() + "\"}");
                } catch (Exception e) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
                }
            }
        }
    }

    // ==========================================
    // 4. CALIBRATIONS API HANDLER
    // ==========================================
    private class CalibrationsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if ("GET".equalsIgnoreCase(method)) {
                List<CalibrationRecord> records = calibrationService.getAllCalibrations();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < records.size(); i++) {
                    CalibrationRecord r = records.get(i);
                    if (i > 0) sb.append(",");
                    sb.append(String.format(
                            "{\"id\":\"%s\",\"deviceId\":\"%s\",\"date\":\"%s\",\"status\":\"%s\",\"tech\":\"%s\"," +
                            "\"certNo\":\"%s\",\"tool\":\"%s\",\"notes\":\"%s\",\"failure\":\"%s\"}",
                            r.getId(), r.getDeviceId(), DateUtils.formatDate(r.getCalibrationDate()), r.getStatus().name(),
                            r.getTechnicianId(), r.getCertificate() != null ? r.getCertificate().getCertificateNumber() : "",
                            escapeJson(r.getTestEquipmentUsed()), escapeJson(r.getNotes()),
                            r.getFailureReason() != null ? escapeJson(r.getFailureReason()) : ""));
                }
                sb.append("]");
                sendJsonResponse(exchange, 200, sb.toString());

            } else if ("POST".equalsIgnoreCase(method)) {
                String body = readRequestBody(exchange);
                Map<String, String> p = parseFormOrJsonParams(body);

                try {
                    authService.login("tech1", "Tech@123");
                    String devId = p.get("deviceId");
                    String techId = p.getOrDefault("technicianId", "TECH-101");
                    String tool = p.getOrDefault("equipment", "Biomedical Analyzer Sim-01");
                    String notes = p.getOrDefault("notes", "Web calibration test");

                    String param = p.getOrDefault("paramName", "Primary Operating Parameter");
                    double nominal = Double.parseDouble(p.getOrDefault("nominal", "100.0"));
                    double measured = Double.parseDouble(p.getOrDefault("measured", "100.0"));
                    String unit = p.getOrDefault("unit", "units");
                    double tolerance = Double.parseDouble(p.getOrDefault("tolerance", "5.0"));

                    CalibrationMeasurement m = new CalibrationMeasurement(param, nominal, measured, unit, tolerance);
                    CalibrationRecord record = calibrationService.performCalibration(devId, techId, List.of(m), tool, notes);

                    BiomedicalDevice d = deviceService.getDeviceById(devId);
                    String resp = String.format(Locale.US,
                            "{\"success\":true,\"status\":\"%s\",\"certNo\":\"%s\",\"deviation\":%.3f,\"allowed\":%.3f," +
                            "\"deviceStatus\":\"%s\",\"failureReason\":\"%s\"}",
                            record.getStatus().name(),
                            record.getCertificate() != null ? record.getCertificate().getCertificateNumber() : "NONE",
                            m.getDeviation(), m.getAllowedTolerance(), d.getDeviceStatus().name(),
                            record.getFailureReason() != null ? escapeJson(record.getFailureReason()) : "");
                    sendJsonResponse(exchange, 200, resp);

                } catch (Exception e) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
                }
            }
        }
    }

    // ==========================================
    // 5. MAINTENANCE API HANDLER
    // ==========================================
    private class MaintenanceApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equalsIgnoreCase(method)) {
                List<MaintenanceRequest> requests = maintenanceService.getAllRequests();
                List<AbstractMaintenanceRecord> records = maintenanceService.getAllRecords();

                StringBuilder sb = new StringBuilder("{\"requests\":[");
                for (int i = 0; i < requests.size(); i++) {
                    MaintenanceRequest r = requests.get(i);
                    if (i > 0) sb.append(",");
                    sb.append(String.format("{\"id\":\"%s\",\"deviceId\":\"%s\",\"type\":\"%s\",\"status\":\"%s\",\"desc\":\"%s\",\"tech\":\"%s\"}",
                            r.getId(), r.getDeviceId(), r.getRequestedType().name(), r.getStatus().name(),
                            escapeJson(r.getIssueDescription()), r.getAssignedTechnicianId()));
                }
                sb.append("],\"records\":[");
                for (int i = 0; i < records.size(); i++) {
                    AbstractMaintenanceRecord rec = records.get(i);
                    if (i > 0) sb.append(",");
                    sb.append(String.format(Locale.US,
                            "{\"id\":\"%s\",\"deviceId\":\"%s\",\"type\":\"%s\",\"status\":\"%s\",\"cost\":%.2f,\"downtime\":%.1f,\"desc\":\"%s\"}",
                            rec.getId(), rec.getDeviceId(), rec.getMaintenanceType().name(), rec.getMaintenanceStatus().name(),
                            rec.getCost().doubleValue(), rec.getDowntimeHours(), escapeJson(rec.getWorkDescription())));
                }
                sb.append("]}");
                sendJsonResponse(exchange, 200, sb.toString());

            } else if ("POST".equalsIgnoreCase(method)) {
                String body = readRequestBody(exchange);
                Map<String, String> p = parseFormOrJsonParams(body);

                try {
                    if (path.endsWith("/request")) {
                        authService.login("staff1", "Staff@123");
                        String devId = p.get("deviceId");
                        MaintenanceType type = MaintenanceType.valueOf(p.getOrDefault("type", "CORRECTIVE"));
                        String desc = p.getOrDefault("description", "Reported malfunction via web");
                        MaintenanceRequest req = maintenanceService.createRequest(devId, type, desc);
                        sendJsonResponse(exchange, 200, "{\"success\":true,\"id\":\"" + req.getId() + "\"}");

                    } else if (path.endsWith("/start")) {
                        authService.login("tech1", "Tech@123");
                        String reqId = p.get("requestId");
                        AbstractMaintenanceRecord rec = maintenanceService.startMaintenance(reqId, "TECH-101");
                        sendJsonResponse(exchange, 200, "{\"success\":true,\"recordId\":\"" + rec.getId() + "\"}");

                    } else if (path.endsWith("/complete")) {
                        authService.login("tech1", "Tech@123");
                        String recordId = p.get("recordId");
                        double cost = Double.parseDouble(p.getOrDefault("cost", "150.0"));
                        double downtime = Double.parseDouble(p.getOrDefault("downtime", "2.0"));
                        String parts = p.getOrDefault("parts", "");
                        List<String> partList = parts.isEmpty() ? List.of() : Arrays.asList(parts.split(","));
                        String notes = p.getOrDefault("notes", "Completed via web console");

                        maintenanceService.completeMaintenance(recordId, BigDecimal.valueOf(cost), downtime, partList, List.of("Safety Test"), notes);
                        sendJsonResponse(exchange, 200, "{\"success\":true}");
                    }
                } catch (Exception e) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
                }
            }
        }
    }

    // ==========================================
    // 6. AUDIT API HANDLER
    // ==========================================
    private class AuditApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<com.hospital.biomedical.model.audit.AuditLog> logs = auditService.getAllLogs();
            StringBuilder sb = new StringBuilder("[");
            int start = Math.max(0, logs.size() - 50);
            for (int i = logs.size() - 1; i >= start; i--) {
                com.hospital.biomedical.model.audit.AuditLog l = logs.get(i);
                if (i < logs.size() - 1) sb.append(",");
                sb.append(String.format("{\"id\":\"%s\",\"timestamp\":\"%s\",\"user\":\"%s\",\"action\":\"%s\",\"target\":\"%s\",\"targetId\":\"%s\",\"details\":\"%s\"}",
                        l.getId(), DateUtils.formatDateTime(l.getCreatedAt()), l.getActingUsername(),
                        l.getActionType(), l.getTargetEntity(), l.getTargetId(), escapeJson(l.getDetails())));
            }
            sb.append("]");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    // ==========================================
    // 7. AUTH API HANDLER
    // ==========================================
    private class AuthApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equalsIgnoreCase(method)) {
                User current = authService.getCurrentUser();
                if (current != null) {
                    String json = String.format("{\"authenticated\":true,\"username\":\"%s\",\"fullName\":\"%s\",\"role\":\"%s\",\"department\":\"%s\"}",
                            current.getUsername(), escapeJson(current.getFullName()), current.getRole().name(), escapeJson(current.getDepartment()));
                    sendJsonResponse(exchange, 200, json);
                } else {
                    sendJsonResponse(exchange, 200, "{\"authenticated\":false}");
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                String body = readRequestBody(exchange);
                Map<String, String> p = parseFormOrJsonParams(body);

                if (path.endsWith("/logout")) {
                    authService.logout();
                    sendJsonResponse(exchange, 200, "{\"success\":true}");
                    return;
                }

                if (path.endsWith("/register")) {
                    String username = p.get("username");
                    String password = p.get("password");
                    String fullName = p.get("fullName");
                    String email = p.get("email");
                    String department = p.get("department");
                    String roleStr = p.getOrDefault("role", "TECHNICIAN");

                    try {
                        UserRole role;
                        try {
                            role = UserRole.valueOf(roleStr.toUpperCase());
                        } catch (Exception ex) {
                            role = UserRole.TECHNICIAN;
                        }
                        User user = authService.registerUser(username, password, fullName, email, department, role);
                        String json = String.format("{\"success\":true,\"username\":\"%s\",\"fullName\":\"%s\",\"role\":\"%s\",\"department\":\"%s\"}",
                                user.getUsername(), escapeJson(user.getFullName()), user.getRole().name(), escapeJson(user.getDepartment()));
                        sendJsonResponse(exchange, 200, json);
                    } catch (Exception e) {
                        sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
                    }
                    return;
                }

                // Default is login
                String identifier = p.getOrDefault("identifier", p.getOrDefault("username", p.get("email")));
                String password = p.get("password");

                try {
                    User user = authService.login(identifier, password);
                    String json = String.format("{\"success\":true,\"username\":\"%s\",\"fullName\":\"%s\",\"role\":\"%s\",\"department\":\"%s\"}",
                            user.getUsername(), escapeJson(user.getFullName()), user.getRole().name(), escapeJson(user.getDepartment()));
                    sendJsonResponse(exchange, 200, json);
                } catch (Exception e) {
                    sendJsonResponse(exchange, 401, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
                }
            }
        }
    }

    // ==========================================
    // 8. REPORTS API HANDLER
    // ==========================================
    private class ReportsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String type = "";
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2 && "type".equalsIgnoreCase(pair[0])) {
                        type = pair[1].toLowerCase();
                    }
                }
            }

            try {
                // Ensure all reports are freshly generated
                java.nio.file.Path targetFile = null;
                String filename = "report.csv";
                String contentType = "text/csv; charset=UTF-8";

                switch (type) {
                    case "compliance" -> {
                        targetFile = reportService.exportComplianceSummaryReport();
                        filename = "compliance_summary_report.txt";
                        contentType = "text/plain; charset=UTF-8";
                    }
                    case "inventory" -> {
                        targetFile = reportService.exportDeviceInventoryReport();
                        filename = "device_inventory_report.csv";
                    }
                    case "calibrations" -> {
                        targetFile = reportService.exportCalibrationHistoryReport();
                        filename = "calibration_history_report.csv";
                    }
                    case "overdue" -> {
                        targetFile = reportService.exportOverdueCalibrationReport();
                        filename = "overdue_calibrations_report.csv";
                    }
                    case "maintenance" -> {
                        targetFile = reportService.exportMaintenanceHistoryReport();
                        filename = "maintenance_history_report.csv";
                    }
                    case "audit" -> {
                        targetFile = reportService.exportAuditLogReport();
                        filename = "audit_logs_report.csv";
                    }
                    default -> {
                        // Refresh all and return catalog
                        reportService.exportComplianceSummaryReport();
                        reportService.exportDeviceInventoryReport();
                        reportService.exportCalibrationHistoryReport();
                        reportService.exportOverdueCalibrationReport();
                        reportService.exportMaintenanceHistoryReport();
                        reportService.exportAuditLogReport();
                    }
                }

                if (targetFile != null && java.nio.file.Files.exists(targetFile)) {
                    byte[] bytes = java.nio.file.Files.readAllBytes(targetFile);
                    exchange.getResponseHeaders().set("Content-Type", contentType);
                    exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                    exchange.getResponseHeaders().set("Content-Length", String.valueOf(bytes.length));
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                } else {
                    // Return list of available download links
                    String json = "{\"success\":true,\"message\":\"All reports generated.\",\"reports\":[" +
                            "{\"type\":\"compliance\",\"name\":\"Hospital Compliance Audit Report\",\"file\":\"compliance_summary_report.txt\",\"format\":\"TXT\"}," +
                            "{\"type\":\"inventory\",\"name\":\"Device Inventory Dossier\",\"file\":\"device_inventory_report.csv\",\"format\":\"CSV\"}," +
                            "{\"type\":\"calibrations\",\"name\":\"Calibration Verification History\",\"file\":\"calibration_history_report.csv\",\"format\":\"CSV\"}," +
                            "{\"type\":\"overdue\",\"name\":\"Overdue Calibrations Roster\",\"file\":\"overdue_calibrations_report.csv\",\"format\":\"CSV\"}," +
                            "{\"type\":\"maintenance\",\"name\":\"Maintenance Work Orders Log\",\"file\":\"maintenance_history_report.csv\",\"format\":\"CSV\"}," +
                            "{\"type\":\"audit\",\"name\":\"Tamper-Evident Audit Trail\",\"file\":\"audit_logs_report.csv\",\"format\":\"CSV\"}" +
                            "]}";
                    sendJsonResponse(exchange, 200, json);
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // ==========================================
    // 9. AI ASSISTANT COPILOT API HANDLER
    // ==========================================
    private class AiAssistantApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            String method = exchange.getRequestMethod();
            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(method)) {
                List<String> suggestions = aiAssistantService.getDefaultSuggestions();
                StringBuilder sb = new StringBuilder("{\"success\":true,\"suggestions\":[");
                for (int i = 0; i < suggestions.size(); i++) {
                    sb.append("\"").append(escapeJson(suggestions.get(i))).append("\"");
                    if (i < suggestions.size() - 1) sb.append(",");
                }
                sb.append("]}");
                sendJsonResponse(exchange, 200, sb.toString());
                return;
            }

            if ("POST".equalsIgnoreCase(method)) {
                try {
                    String body = readRequestBody(exchange);
                    Map<String, String> p = parseFormOrJsonParams(body);
                    String message = p.getOrDefault("message", p.getOrDefault("prompt", ""));

                    User currentUser = authService.getCurrentUser();
                    BiomedicalAiAssistantService.AiResponse aiResp = aiAssistantService.askAi(message, currentUser);

                    StringBuilder sb = new StringBuilder();
                    sb.append("{");
                    sb.append("\"success\":true,");
                    sb.append("\"reply\":\"").append(escapeJson(aiResp.reply())).append("\",");
                    sb.append("\"engine\":\"").append(escapeJson(aiResp.engine())).append("\",");
                    sb.append("\"suggestions\":[");
                    List<String> sugg = aiResp.suggestions();
                    if (sugg != null) {
                        for (int i = 0; i < sugg.size(); i++) {
                            sb.append("\"").append(escapeJson(sugg.get(i))).append("\"");
                            if (i < sugg.size() - 1) sb.append(",");
                        }
                    }
                    sb.append("]");
                    sb.append("}");

                    sendJsonResponse(exchange, 200, sb.toString());
                } catch (Exception e) {
                    sendJsonResponse(exchange, 500, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
                }
            } else {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
