package com.hospital.biomedical.model.maintenance;

import com.hospital.biomedical.interfaces.Auditable;
import com.hospital.biomedical.interfaces.Reportable;
import com.hospital.biomedical.model.common.BaseEntity;
import com.hospital.biomedical.model.enums.MaintenanceStatus;
import com.hospital.biomedical.model.enums.MaintenanceType;

import java.time.LocalDateTime;

/**
 * Maintenance Work Request initiated by clinical staff or scheduled preventive routine.
 */
public class MaintenanceRequest extends BaseEntity implements Auditable, Reportable {
    private String deviceId;
    private String requestedByUsername;
    private MaintenanceType requestedType;
    private String issueDescription;
    private MaintenanceStatus status;
    private String assignedTechnicianId;
    private String maintenanceRecordId;
    private LocalDateTime requestedAt;
    private LocalDateTime scheduledAt;

    public MaintenanceRequest() {
        super();
        this.status = MaintenanceStatus.REQUESTED;
        this.requestedAt = LocalDateTime.now();
        this.assignedTechnicianId = "UNASSIGNED";
    }

    public MaintenanceRequest(String id, String deviceId, String requestedByUsername,
                              MaintenanceType requestedType, String issueDescription) {
        super(id);
        setDeviceId(deviceId);
        setRequestedByUsername(requestedByUsername);
        this.requestedType = requestedType != null ? requestedType : MaintenanceType.CORRECTIVE;
        setIssueDescription(issueDescription);
        this.status = MaintenanceStatus.REQUESTED;
        this.requestedAt = LocalDateTime.now();
        this.assignedTechnicianId = "UNASSIGNED";
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

    public String getRequestedByUsername() {
        return requestedByUsername;
    }

    public void setRequestedByUsername(String requestedByUsername) {
        this.requestedByUsername = (requestedByUsername != null && !requestedByUsername.trim().isEmpty())
                ? requestedByUsername.trim() : "staff";
    }

    public MaintenanceType getRequestedType() {
        return requestedType;
    }

    public void setRequestedType(MaintenanceType requestedType) {
        this.requestedType = requestedType;
    }

    public String getIssueDescription() {
        return issueDescription;
    }

    public void setIssueDescription(String issueDescription) {
        if (issueDescription == null || issueDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Issue description cannot be blank.");
        }
        this.issueDescription = issueDescription.trim();
    }

    public MaintenanceStatus getStatus() {
        return status;
    }

    public void setStatus(MaintenanceStatus status) {
        this.status = status;
        touchUpdated();
    }

    public String getAssignedTechnicianId() {
        return assignedTechnicianId;
    }

    public void setAssignedTechnicianId(String assignedTechnicianId) {
        this.assignedTechnicianId = (assignedTechnicianId != null && !assignedTechnicianId.trim().isEmpty())
                ? assignedTechnicianId.trim() : "UNASSIGNED";
    }

    public String getMaintenanceRecordId() {
        return maintenanceRecordId;
    }

    public void setMaintenanceRecordId(String maintenanceRecordId) {
        this.maintenanceRecordId = maintenanceRecordId;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    @Override
    public String getEntityIdentifier() {
        return getId();
    }

    @Override
    public String getEntityTypeName() {
        return "MaintenanceRequest";
    }

    @Override
    public String toAuditSummary() {
        return String.format("Request: %s for Device %s, Type: %s, Status: %s, RequestedBy: %s",
                getId(), deviceId, requestedType, status, requestedByUsername);
    }

    @Override
    public String[] getReportHeaders() {
        return new String[]{"Req ID", "Device ID", "Requested By", "Type", "Status", "Assigned Tech", "Description"};
    }

    @Override
    public String[] toReportRow() {
        return new String[]{
                getId(),
                deviceId,
                requestedByUsername,
                requestedType.name(),
                status.name(),
                assignedTechnicianId,
                issueDescription
        };
    }
}
