package com.hospital.biomedical.model.maintenance;

import com.hospital.biomedical.interfaces.Auditable;
import com.hospital.biomedical.interfaces.Reportable;
import com.hospital.biomedical.model.common.BaseEntity;
import com.hospital.biomedical.model.enums.MaintenanceStatus;
import com.hospital.biomedical.model.enums.MaintenanceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract maintenance record establishing base state machine, downtime, and cost encapsulation.
 * Demonstrates Abstraction, Inheritance, and Encapsulation.
 */
public abstract class AbstractMaintenanceRecord extends BaseEntity implements Auditable, Reportable {
    private String deviceId;
    private String technicianId;
    private MaintenanceType maintenanceType;
    private MaintenanceStatus maintenanceStatus;
    private String workDescription;
    private List<String> partsReplaced;
    private List<String> checklistItems;
    private BigDecimal cost;
    private double downtimeHours;
    private LocalDateTime scheduledDateTime;
    private LocalDateTime startedDateTime;
    private LocalDateTime completedDateTime;
    private String resolutionNotes;

    public AbstractMaintenanceRecord() {
        super();
        this.maintenanceStatus = MaintenanceStatus.REQUESTED;
        this.partsReplaced = new ArrayList<>();
        this.checklistItems = new ArrayList<>();
        this.cost = BigDecimal.ZERO;
        this.downtimeHours = 0.0;
    }

    public AbstractMaintenanceRecord(String id, String deviceId, String technicianId,
                                   MaintenanceType maintenanceType, String workDescription) {
        super(id);
        setDeviceId(deviceId);
        setTechnicianId(technicianId);
        setMaintenanceType(maintenanceType);
        setWorkDescription(workDescription);
        this.maintenanceStatus = MaintenanceStatus.REQUESTED;
        this.partsReplaced = new ArrayList<>();
        this.checklistItems = new ArrayList<>();
        this.cost = BigDecimal.ZERO;
        this.downtimeHours = 0.0;
        this.scheduledDateTime = LocalDateTime.now();
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID cannot be blank.");
        }
        this.deviceId = deviceId.trim();
    }

    public String getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(String technicianId) {
        this.technicianId = (technicianId != null && !technicianId.trim().isEmpty()) ? technicianId.trim() : "UNASSIGNED";
    }

    public MaintenanceType getMaintenanceType() {
        return maintenanceType;
    }

    public void setMaintenanceType(MaintenanceType maintenanceType) {
        if (maintenanceType == null) {
            throw new IllegalArgumentException("Maintenance type cannot be null.");
        }
        this.maintenanceType = maintenanceType;
    }

    public MaintenanceStatus getMaintenanceStatus() {
        return maintenanceStatus;
    }

    public void setMaintenanceStatus(MaintenanceStatus maintenanceStatus) {
        this.maintenanceStatus = maintenanceStatus;
        touchUpdated();
    }

    public String getWorkDescription() {
        return workDescription;
    }

    public void setWorkDescription(String workDescription) {
        this.workDescription = (workDescription != null && !workDescription.trim().isEmpty()) ? workDescription.trim() : "Maintenance servicing";
    }

    public List<String> getPartsReplaced() {
        return Collections.unmodifiableList(partsReplaced);
    }

    public void addPartReplaced(String part) {
        if (part != null && !part.trim().isEmpty()) {
            this.partsReplaced.add(part.trim());
        }
    }

    public void setPartsReplaced(List<String> parts) {
        this.partsReplaced = parts != null ? new ArrayList<>(parts) : new ArrayList<>();
    }

    public List<String> getChecklistItems() {
        return Collections.unmodifiableList(checklistItems);
    }

    public void addChecklistItem(String item) {
        if (item != null && !item.trim().isEmpty()) {
            this.checklistItems.add(item.trim());
        }
    }

    public void setChecklistItems(List<String> items) {
        this.checklistItems = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        if (cost == null || cost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Maintenance cost cannot be negative.");
        }
        this.cost = cost;
    }

    public double getDowntimeHours() {
        return downtimeHours;
    }

    public void setDowntimeHours(double downtimeHours) {
        if (downtimeHours < 0) {
            throw new IllegalArgumentException("Equipment downtime cannot be negative.");
        }
        this.downtimeHours = downtimeHours;
    }

    public LocalDateTime getScheduledDateTime() {
        return scheduledDateTime;
    }

    public void setScheduledDateTime(LocalDateTime scheduledDateTime) {
        this.scheduledDateTime = scheduledDateTime;
    }

    public LocalDateTime getStartedDateTime() {
        return startedDateTime;
    }

    public void setStartedDateTime(LocalDateTime startedDateTime) {
        this.startedDateTime = startedDateTime;
    }

    public LocalDateTime getCompletedDateTime() {
        return completedDateTime;
    }

    public void setCompletedDateTime(LocalDateTime completedDateTime) {
        this.completedDateTime = completedDateTime;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    // Abstract Polymorphic Contracts
    public abstract boolean requiresRecalibration();
    public abstract double calculateUrgencyMultiplier();

    @Override
    public String getEntityIdentifier() {
        return getId();
    }

    @Override
    public String getEntityTypeName() {
        return "MaintenanceRecord";
    }

    @Override
    public String toAuditSummary() {
        return String.format("Maintenance: %s [%s] on Device %s, Status: %s, Cost: $%s, Downtime: %.1fh",
                getId(), maintenanceType, deviceId, maintenanceStatus, cost, downtimeHours);
    }

    @Override
    public String[] getReportHeaders() {
        return new String[]{"Record ID", "Device ID", "Type", "Status", "Technician", "Cost", "Downtime (h)", "Scheduled", "Completed"};
    }

    @Override
    public String[] toReportRow() {
        return new String[]{
                getId(),
                getDeviceId(),
                getMaintenanceType().name(),
                getMaintenanceStatus().name(),
                getTechnicianId(),
                "$" + getCost().toPlainString(),
                String.format("%.1f", getDowntimeHours()),
                getScheduledDateTime() != null ? getScheduledDateTime().toLocalDate().toString() : "N/A",
                getCompletedDateTime() != null ? getCompletedDateTime().toLocalDate().toString() : "N/A"
        };
    }
}
