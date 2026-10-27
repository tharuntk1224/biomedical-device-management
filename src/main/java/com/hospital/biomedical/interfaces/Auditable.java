package com.hospital.biomedical.interfaces;

/**
 * Interface contract for entities that can be recorded in audit logs.
 */
public interface Auditable {
    String getEntityIdentifier();
    String getEntityTypeName();
    String toAuditSummary();
}
