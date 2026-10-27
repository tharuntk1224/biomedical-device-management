package com.hospital.biomedical.service;

import com.hospital.biomedical.model.audit.AuditLog;
import com.hospital.biomedical.repository.AuditRepository;
import com.hospital.biomedical.util.IdGenerator;

import java.util.List;

/**
 * Service managing system-wide audit trail recording.
 */
public class AuditService {
    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void log(String actingUsername, String actionType, String targetEntity, String targetId, String details) {
        String eventId = IdGenerator.nextAuditId();
        AuditLog log = new AuditLog(eventId, actingUsername, actionType, targetEntity, targetId, details);
        auditRepository.save(log);
    }

    public List<AuditLog> getAllLogs() {
        return auditRepository.findAll();
    }

    public List<AuditLog> getLogsByEntity(String entity) {
        return auditRepository.findByTargetEntity(entity);
    }

    public List<AuditLog> getLogsByUser(String username) {
        return auditRepository.findByActingUser(username);
    }
}
