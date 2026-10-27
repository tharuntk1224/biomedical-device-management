package com.hospital.biomedical.model.audit;

import com.hospital.biomedical.interfaces.Reportable;
import com.hospital.biomedical.model.common.BaseEntity;

import java.time.LocalDateTime;

/**
 * Immutable audit record capturing operational and safety actions.
 */
public class AuditLog extends BaseEntity implements Reportable {
    private String actingUsername;
    private String actionType;
    private String targetEntity;
    private String targetId;
    private String details;

    public AuditLog() {
        super();
    }

    public AuditLog(String id, String actingUsername, String actionType,
                    String targetEntity, String targetId, String details) {
        super(id);
        this.actingUsername = actingUsername != null ? actingUsername : "SYSTEM";
        this.actionType = actionType != null ? actionType : "UNKNOWN";
        this.targetEntity = targetEntity != null ? targetEntity : "GENERAL";
        this.targetId = targetId != null ? targetId : "N/A";
        this.details = details != null ? details : "";
    }

    public AuditLog(String id, LocalDateTime timestamp, String actingUsername,
                    String actionType, String targetEntity, String targetId, String details) {
        super(id, timestamp, timestamp);
        this.actingUsername = actingUsername != null ? actingUsername : "SYSTEM";
        this.actionType = actionType != null ? actionType : "UNKNOWN";
        this.targetEntity = targetEntity != null ? targetEntity : "GENERAL";
        this.targetId = targetId != null ? targetId : "N/A";
        this.details = details != null ? details : "";
    }

    public String getActingUsername() {
        return actingUsername;
    }

    public void setActingUsername(String actingUsername) {
        this.actingUsername = actingUsername;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getTargetEntity() {
        return targetEntity;
    }

    public void setTargetEntity(String targetEntity) {
        this.targetEntity = targetEntity;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public String[] getReportHeaders() {
        return new String[]{"Event ID", "Timestamp", "User", "Action", "Target", "Target ID", "Details"};
    }

    @Override
    public String[] toReportRow() {
        return new String[]{
                getId(),
                getCreatedAt().toString(),
                actingUsername,
                actionType,
                targetEntity,
                targetId,
                details
        };
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | User: %s | Action: %s | Target: %s (%s) | %s",
                getId(), getCreatedAt(), actingUsername, actionType, targetEntity, targetId, details);
    }
}
