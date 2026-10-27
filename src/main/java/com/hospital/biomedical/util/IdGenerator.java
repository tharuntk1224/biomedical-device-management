package com.hospital.biomedical.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe unique identifier generator for biomedical entities.
 */
public final class IdGenerator {
    private static final AtomicLong DEVICE_SEQ = new AtomicLong(1000);
    private static final AtomicLong CALIBRATION_SEQ = new AtomicLong(2000);
    private static final AtomicLong MAINTENANCE_SEQ = new AtomicLong(3000);
    private static final AtomicLong REQUEST_SEQ = new AtomicLong(4000);
    private static final AtomicLong AUDIT_SEQ = new AtomicLong(5000);
    private static final AtomicLong CERTIFICATE_SEQ = new AtomicLong(8000);

    private IdGenerator() {}

    public static String nextDeviceId() {
        return "DEV-" + DEVICE_SEQ.incrementAndGet();
    }

    public static String nextCalibrationId() {
        return "CAL-" + CALIBRATION_SEQ.incrementAndGet();
    }

    public static String nextMaintenanceId() {
        return "MNT-" + MAINTENANCE_SEQ.incrementAndGet();
    }

    public static String nextRequestId() {
        return "REQ-" + REQUEST_SEQ.incrementAndGet();
    }

    public static String nextAuditId() {
        return "AUD-" + AUDIT_SEQ.incrementAndGet();
    }

    public static String nextCertificateNumber() {
        return "CERT-" + java.time.Year.now().getValue() + "-" + CERTIFICATE_SEQ.incrementAndGet();
    }

    public static void updateDeviceSequence(long lastSeen) {
        DEVICE_SEQ.updateAndGet(cur -> Math.max(cur, lastSeen));
    }

    public static void updateCalibrationSequence(long lastSeen) {
        CALIBRATION_SEQ.updateAndGet(cur -> Math.max(cur, lastSeen));
    }

    public static void updateMaintenanceSequence(long lastSeen) {
        MAINTENANCE_SEQ.updateAndGet(cur -> Math.max(cur, lastSeen));
    }

    public static void updateRequestSequence(long lastSeen) {
        REQUEST_SEQ.updateAndGet(cur -> Math.max(cur, lastSeen));
    }

    public static void updateAuditSequence(long lastSeen) {
        AUDIT_SEQ.updateAndGet(cur -> Math.max(cur, lastSeen));
    }
}
