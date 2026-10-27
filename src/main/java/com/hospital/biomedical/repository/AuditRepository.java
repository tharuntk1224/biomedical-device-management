package com.hospital.biomedical.repository;

import com.hospital.biomedical.model.audit.AuditLog;
import com.hospital.biomedical.util.CsvUtils;
import com.hospital.biomedical.util.DateUtils;
import com.hospital.biomedical.util.IdGenerator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data access and persistence repository for AuditLog records.
 */
public class AuditRepository {
    private final List<AuditLog> auditLogs = new ArrayList<>();
    private final Path storagePath;

    public AuditRepository() {
        this(Paths.get("data", "audit_logs.csv"));
    }

    public AuditRepository(Path storagePath) {
        this.storagePath = storagePath;
        load();
    }

    public synchronized void save(AuditLog log) {
        Objects.requireNonNull(log, "AuditLog cannot be null.");
        auditLogs.add(log);
        flush();
    }

    public synchronized List<AuditLog> findAll() {
        return new ArrayList<>(auditLogs);
    }

    public synchronized List<AuditLog> findByTargetEntity(String entity) {
        if (entity == null) return Collections.emptyList();
        return auditLogs.stream()
                .filter(l -> l.getTargetEntity().equalsIgnoreCase(entity))
                .collect(Collectors.toList());
    }

    public synchronized List<AuditLog> findByActingUser(String username) {
        if (username == null) return Collections.emptyList();
        return auditLogs.stream()
                .filter(l -> l.getActingUsername().equalsIgnoreCase(username))
                .collect(Collectors.toList());
    }

    public synchronized void clear() {
        auditLogs.clear();
        flush();
    }

    private synchronized void flush() {
        List<String> lines = new ArrayList<>();
        lines.add(CsvUtils.toCsvLine("id", "timestamp", "actingUser", "actionType", "targetEntity", "targetId", "details"));

        for (AuditLog l : auditLogs) {
            lines.add(CsvUtils.toCsvLine(
                    l.getId(),
                    DateUtils.formatDateTime(l.getCreatedAt()),
                    l.getActingUsername(),
                    l.getActionType(),
                    l.getTargetEntity(),
                    l.getTargetId(),
                    l.getDetails()
            ));
        }

        try {
            CsvUtils.writeLinesAtomic(storagePath, lines);
        } catch (IOException e) {
            System.err.println("Error saving audit logs: " + e.getMessage());
        }
    }

    private synchronized void load() {
        auditLogs.clear();
        List<String> lines = CsvUtils.readLinesSafe(storagePath);
        if (lines.isEmpty()) return;

        boolean isFirst = true;
        for (String line : lines) {
            if (isFirst) {
                isFirst = false;
                continue;
            }
            List<String> cols = CsvUtils.parseCsvLine(line);
            if (cols.size() < 7) continue;

            try {
                String id = cols.get(0);
                LocalDateTime ts = DateUtils.parseDateTime(cols.get(1));
                String user = cols.get(2);
                String action = cols.get(3);
                String entity = cols.get(4);
                String targetId = cols.get(5);
                String details = cols.get(6);

                AuditLog log = new AuditLog(id, ts, user, action, entity, targetId, details);
                auditLogs.add(log);

                if (id.startsWith("AUD-")) {
                    try {
                        long num = Long.parseLong(id.substring(4));
                        IdGenerator.updateAuditSequence(num);
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                System.err.println("Warning: Corrupt audit log skipped: " + e.getMessage());
            }
        }
    }
}
