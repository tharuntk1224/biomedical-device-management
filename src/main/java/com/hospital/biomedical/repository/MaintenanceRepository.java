package com.hospital.biomedical.repository;

import com.hospital.biomedical.model.enums.MaintenanceStatus;
import com.hospital.biomedical.model.enums.MaintenanceType;
import com.hospital.biomedical.model.maintenance.*;
import com.hospital.biomedical.util.CsvUtils;
import com.hospital.biomedical.util.DateUtils;
import com.hospital.biomedical.util.IdGenerator;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data access and persistence repository for MaintenanceRecord and MaintenanceRequest entities.
 */
public class MaintenanceRepository {
    private final Map<String, AbstractMaintenanceRecord> recordsById = new LinkedHashMap<>();
    private final Map<String, MaintenanceRequest> requestsById = new LinkedHashMap<>();
    private final Path recordsPath;
    private final Path requestsPath;

    public MaintenanceRepository() {
        this(Paths.get("data", "maintenance_records.csv"), Paths.get("data", "maintenance_requests.csv"));
    }

    public MaintenanceRepository(Path recordsPath, Path requestsPath) {
        this.recordsPath = recordsPath;
        this.requestsPath = requestsPath;
        load();
    }

    // Maintenance Record CRUD
    public synchronized void saveRecord(AbstractMaintenanceRecord record) {
        Objects.requireNonNull(record, "Maintenance record cannot be null.");
        recordsById.put(record.getId(), record);
        flushRecords();
    }

    public synchronized Optional<AbstractMaintenanceRecord> findRecordById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(recordsById.get(id.trim().toUpperCase()));
    }

    public synchronized List<AbstractMaintenanceRecord> findAllRecords() {
        return new ArrayList<>(recordsById.values());
    }

    public synchronized List<AbstractMaintenanceRecord> findRecordsByDeviceId(String deviceId) {
        if (deviceId == null) return Collections.emptyList();
        String devId = deviceId.trim().toUpperCase();
        return recordsById.values().stream()
                .filter(r -> r.getDeviceId().equalsIgnoreCase(devId))
                .collect(Collectors.toList());
    }

    public synchronized List<AbstractMaintenanceRecord> findRecordsByStatus(MaintenanceStatus status) {
        return recordsById.values().stream()
                .filter(r -> r.getMaintenanceStatus() == status)
                .collect(Collectors.toList());
    }

    // Maintenance Request CRUD
    public synchronized void saveRequest(MaintenanceRequest request) {
        Objects.requireNonNull(request, "Maintenance request cannot be null.");
        requestsById.put(request.getId(), request);
        flushRequests();
    }

    public synchronized Optional<MaintenanceRequest> findRequestById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(requestsById.get(id.trim().toUpperCase()));
    }

    public synchronized List<MaintenanceRequest> findAllRequests() {
        return new ArrayList<>(requestsById.values());
    }

    public synchronized List<MaintenanceRequest> findRequestsByDeviceId(String deviceId) {
        if (deviceId == null) return Collections.emptyList();
        String devId = deviceId.trim().toUpperCase();
        return requestsById.values().stream()
                .filter(r -> r.getDeviceId().equalsIgnoreCase(devId))
                .collect(Collectors.toList());
    }

    public synchronized List<MaintenanceRequest> findRequestsByStatus(MaintenanceStatus status) {
        return requestsById.values().stream()
                .filter(r -> r.getStatus() == status)
                .collect(Collectors.toList());
    }

    public synchronized void clear() {
        recordsById.clear();
        requestsById.clear();
        flushRecords();
        flushRequests();
    }

    private synchronized void flushRecords() {
        List<String> lines = new ArrayList<>();
        lines.add(CsvUtils.toCsvLine("type", "id", "deviceId", "technicianId", "maintType", "status",
                "cost", "downtimeHours", "workDesc", "parts", "checklist", "scheduled", "started", "completed", "resolutionNotes"));

        for (AbstractMaintenanceRecord r : recordsById.values()) {
            lines.add(CsvUtils.toCsvLine(
                    r.getClass().getSimpleName(),
                    r.getId(),
                    r.getDeviceId(),
                    r.getTechnicianId(),
                    r.getMaintenanceType().name(),
                    r.getMaintenanceStatus().name(),
                    r.getCost().toPlainString(),
                    String.valueOf(r.getDowntimeHours()),
                    r.getWorkDescription(),
                    String.join(";", r.getPartsReplaced()),
                    String.join(";", r.getChecklistItems()),
                    DateUtils.formatDateTime(r.getScheduledDateTime()),
                    DateUtils.formatDateTime(r.getStartedDateTime()),
                    DateUtils.formatDateTime(r.getCompletedDateTime()),
                    r.getResolutionNotes() != null ? r.getResolutionNotes() : ""
            ));
        }

        try {
            CsvUtils.writeLinesAtomic(recordsPath, lines);
        } catch (IOException e) {
            System.err.println("Error saving maintenance records: " + e.getMessage());
        }
    }

    private synchronized void flushRequests() {
        List<String> lines = new ArrayList<>();
        lines.add(CsvUtils.toCsvLine("id", "deviceId", "requestedBy", "type", "description", "status",
                "assignedTech", "recordId", "requestedAt", "scheduledAt"));

        for (MaintenanceRequest req : requestsById.values()) {
            lines.add(CsvUtils.toCsvLine(
                    req.getId(),
                    req.getDeviceId(),
                    req.getRequestedByUsername(),
                    req.getRequestedType().name(),
                    req.getIssueDescription(),
                    req.getStatus().name(),
                    req.getAssignedTechnicianId(),
                    req.getMaintenanceRecordId() != null ? req.getMaintenanceRecordId() : "",
                    DateUtils.formatDateTime(req.getRequestedAt()),
                    DateUtils.formatDateTime(req.getScheduledAt())
            ));
        }

        try {
            CsvUtils.writeLinesAtomic(requestsPath, lines);
        } catch (IOException e) {
            System.err.println("Error saving maintenance requests: " + e.getMessage());
        }
    }

    private synchronized void load() {
        recordsById.clear();
        requestsById.clear();

        // Load records
        List<String> rLines = CsvUtils.readLinesSafe(recordsPath);
        if (!rLines.isEmpty()) {
            boolean isFirst = true;
            for (String line : rLines) {
                if (isFirst) {
                    isFirst = false;
                    continue;
                }
                List<String> cols = CsvUtils.parseCsvLine(line);
                if (cols.size() < 9) continue;

                try {
                    String type = cols.get(0);
                    String id = cols.get(1);
                    String deviceId = cols.get(2);
                    String techId = cols.get(3);
                    MaintenanceType mType = MaintenanceType.valueOf(cols.get(4));
                    MaintenanceStatus status = MaintenanceStatus.valueOf(cols.get(5));
                    BigDecimal cost = new BigDecimal(cols.get(6));
                    double downtime = Double.parseDouble(cols.get(7));
                    String desc = cols.get(8);

                    AbstractMaintenanceRecord record;
                    if ("EmergencyMaintenance".equals(type)) {
                        record = new EmergencyMaintenance(id, deviceId, techId, desc, "Emergency intervention", false, "Staff");
                    } else if ("CorrectiveMaintenance".equals(type)) {
                        record = new CorrectiveMaintenance(id, deviceId, techId, desc, "Defect Repair", "General");
                    } else {
                        record = new PreventiveMaintenance(id, deviceId, techId, desc, 6);
                    }

                    record.setMaintenanceStatus(status);
                    record.setCost(cost);
                    record.setDowntimeHours(downtime);

                    if (cols.size() > 9 && !cols.get(9).isEmpty()) {
                        record.setPartsReplaced(Arrays.asList(cols.get(9).split(";")));
                    }
                    if (cols.size() > 10 && !cols.get(10).isEmpty()) {
                        record.setChecklistItems(Arrays.asList(cols.get(10).split(";")));
                    }
                    if (cols.size() > 11) record.setScheduledDateTime(DateUtils.parseDateTime(cols.get(11)));
                    if (cols.size() > 12) record.setStartedDateTime(DateUtils.parseDateTime(cols.get(12)));
                    if (cols.size() > 13) record.setCompletedDateTime(DateUtils.parseDateTime(cols.get(13)));
                    if (cols.size() > 14) record.setResolutionNotes(cols.get(14));

                    recordsById.put(record.getId(), record);

                    if (id.startsWith("MNT-")) {
                        try {
                            long num = Long.parseLong(id.substring(4));
                            IdGenerator.updateMaintenanceSequence(num);
                        } catch (Exception ignored) {}
                    }
                } catch (Exception e) {
                    System.err.println("Warning: Corrupt maintenance record skipped: " + e.getMessage());
                }
            }
        }

        // Load requests
        List<String> reqLines = CsvUtils.readLinesSafe(requestsPath);
        if (!reqLines.isEmpty()) {
            boolean isFirst = true;
            for (String line : reqLines) {
                if (isFirst) {
                    isFirst = false;
                    continue;
                }
                List<String> cols = CsvUtils.parseCsvLine(line);
                if (cols.size() < 6) continue;

                try {
                    String id = cols.get(0);
                    String deviceId = cols.get(1);
                    String reqBy = cols.get(2);
                    MaintenanceType mType = MaintenanceType.valueOf(cols.get(3));
                    String desc = cols.get(4);
                    MaintenanceStatus status = MaintenanceStatus.valueOf(cols.get(5));

                    MaintenanceRequest req = new MaintenanceRequest(id, deviceId, reqBy, mType, desc);
                    req.setStatus(status);
                    if (cols.size() > 6) req.setAssignedTechnicianId(cols.get(6));
                    if (cols.size() > 7) req.setMaintenanceRecordId(cols.get(7));
                    if (cols.size() > 8) req.setRequestedAt(DateUtils.parseDateTime(cols.get(8)));
                    if (cols.size() > 9) req.setScheduledAt(DateUtils.parseDateTime(cols.get(9)));

                    requestsById.put(req.getId(), req);

                    if (id.startsWith("REQ-")) {
                        try {
                            long num = Long.parseLong(id.substring(4));
                            IdGenerator.updateRequestSequence(num);
                        } catch (Exception ignored) {}
                    }
                } catch (Exception e) {
                    System.err.println("Warning: Corrupt maintenance request skipped: " + e.getMessage());
                }
            }
        }
    }
}
