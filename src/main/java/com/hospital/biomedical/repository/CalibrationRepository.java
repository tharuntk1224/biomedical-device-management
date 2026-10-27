package com.hospital.biomedical.repository;

import com.hospital.biomedical.model.calibration.CalibrationCertificate;
import com.hospital.biomedical.model.calibration.CalibrationMeasurement;
import com.hospital.biomedical.model.calibration.CalibrationRecord;
import com.hospital.biomedical.model.enums.CalibrationStatus;
import com.hospital.biomedical.util.CsvUtils;
import com.hospital.biomedical.util.DateUtils;
import com.hospital.biomedical.util.IdGenerator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data access and persistence repository for CalibrationRecord entities.
 */
public class CalibrationRepository {
    private final Map<String, CalibrationRecord> recordsById = new LinkedHashMap<>();
    private final Path recordsPath;
    private final Path measurementsPath;

    public CalibrationRepository() {
        this(Paths.get("data", "calibrations.csv"), Paths.get("data", "calibration_measurements.csv"));
    }

    public CalibrationRepository(Path recordsPath, Path measurementsPath) {
        this.recordsPath = recordsPath;
        this.measurementsPath = measurementsPath;
        load();
    }

    public synchronized void save(CalibrationRecord record) {
        Objects.requireNonNull(record, "CalibrationRecord cannot be null.");
        recordsById.put(record.getId(), record);
        flush();
    }

    public synchronized Optional<CalibrationRecord> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(recordsById.get(id.trim().toUpperCase()));
    }

    public synchronized List<CalibrationRecord> findAll() {
        return new ArrayList<>(recordsById.values());
    }

    public synchronized List<CalibrationRecord> findByDeviceId(String deviceId) {
        if (deviceId == null) return Collections.emptyList();
        String devId = deviceId.trim().toUpperCase();
        return recordsById.values().stream()
                .filter(r -> r.getDeviceId().equalsIgnoreCase(devId))
                .sorted((a, b) -> b.getCalibrationDate().compareTo(a.getCalibrationDate()))
                .collect(Collectors.toList());
    }

    public synchronized List<CalibrationRecord> findByStatus(CalibrationStatus status) {
        return recordsById.values().stream()
                .filter(r -> r.getStatus() == status)
                .collect(Collectors.toList());
    }

    public synchronized int count() {
        return recordsById.size();
    }

    public synchronized void clear() {
        recordsById.clear();
        flush();
    }

    private synchronized void flush() {
        List<String> recordLines = new ArrayList<>();
        recordLines.add(CsvUtils.toCsvLine("id", "deviceId", "technicianId", "calibrationDate", "status",
                "testEquipment", "notes", "failureReason", "certNo", "certIssued", "certValidUntil", "certTechName"));

        List<String> measLines = new ArrayList<>();
        measLines.add(CsvUtils.toCsvLine("calibrationId", "parameterName", "nominal", "measured", "unit", "tolerance", "deviation", "passed"));

        for (CalibrationRecord r : recordsById.values()) {
            CalibrationCertificate c = r.getCertificate();
            recordLines.add(CsvUtils.toCsvLine(
                    r.getId(),
                    r.getDeviceId(),
                    r.getTechnicianId(),
                    DateUtils.formatDate(r.getCalibrationDate()),
                    r.getStatus().name(),
                    r.getTestEquipmentUsed(),
                    r.getNotes(),
                    r.getFailureReason(),
                    c != null ? c.getCertificateNumber() : "",
                    c != null ? DateUtils.formatDate(c.getIssuedDate()) : "",
                    c != null ? DateUtils.formatDate(c.getValidUntil()) : "",
                    c != null ? c.getTechnicianName() : ""
            ));

            for (CalibrationMeasurement m : r.getMeasurements()) {
                measLines.add(CsvUtils.toCsvLine(
                        r.getId(),
                        m.getParameterName(),
                        String.valueOf(m.getNominalValue()),
                        String.valueOf(m.getMeasuredValue()),
                        m.getUnit(),
                        String.valueOf(m.getAllowedTolerance()),
                        String.valueOf(m.getDeviation()),
                        String.valueOf(m.isPassed())
                ));
            }
        }

        try {
            CsvUtils.writeLinesAtomic(recordsPath, recordLines);
            CsvUtils.writeLinesAtomic(measurementsPath, measLines);
        } catch (IOException e) {
            System.err.println("Error saving calibration records: " + e.getMessage());
        }
    }

    private synchronized void load() {
        recordsById.clear();
        List<String> rLines = CsvUtils.readLinesSafe(recordsPath);
        if (rLines.isEmpty()) return;

        boolean isFirst = true;
        for (String line : rLines) {
            if (isFirst) {
                isFirst = false;
                continue;
            }
            List<String> cols = CsvUtils.parseCsvLine(line);
            if (cols.size() < 5) continue;

            try {
                String id = cols.get(0);
                String deviceId = cols.get(1);
                String techId = cols.get(2);
                LocalDate calDate = DateUtils.parseDate(cols.get(3));
                CalibrationStatus status = CalibrationStatus.valueOf(cols.get(4));
                String equipment = cols.size() > 5 ? cols.get(5) : "Multimeter";
                String notes = cols.size() > 6 ? cols.get(6) : "";
                String failReason = cols.size() > 7 ? cols.get(7) : null;
                String certNo = cols.size() > 8 ? cols.get(8) : "";

                CalibrationRecord record = new CalibrationRecord(id, deviceId, techId, calDate, equipment, notes);
                record.setStatus(status);
                record.setFailureReason(failReason);

                if (!certNo.isEmpty()) {
                    LocalDate issued = cols.size() > 9 ? DateUtils.parseDate(cols.get(9)) : calDate;
                    LocalDate validUntil = cols.size() > 10 ? DateUtils.parseDate(cols.get(10)) : calDate.plusMonths(6);
                    String certTech = cols.size() > 11 ? cols.get(11) : techId;
                    CalibrationCertificate cert = new CalibrationCertificate(certNo, deviceId, "Device", "ASSET",
                            techId, certTech, issued, validUntil, "Hospital Standard SIM-01");
                    record.setCertificate(cert);
                }

                recordsById.put(record.getId(), record);

                if (id.startsWith("CAL-")) {
                    try {
                        long num = Long.parseLong(id.substring(4));
                        IdGenerator.updateCalibrationSequence(num);
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                System.err.println("Warning: Corrupt calibration record skipped: " + e.getMessage());
            }
        }

        // Now attach measurements
        List<String> mLines = CsvUtils.readLinesSafe(measurementsPath);
        if (!mLines.isEmpty()) {
            isFirst = true;
            for (String line : mLines) {
                if (isFirst) {
                    isFirst = false;
                    continue;
                }
                List<String> cols = CsvUtils.parseCsvLine(line);
                if (cols.size() < 6) continue;

                String calId = cols.get(0);
                CalibrationRecord record = recordsById.get(calId);
                if (record != null) {
                    try {
                        String param = cols.get(1);
                        double nominal = Double.parseDouble(cols.get(2));
                        double measured = Double.parseDouble(cols.get(3));
                        String unit = cols.get(4);
                        double tol = Double.parseDouble(cols.get(5));
                        CalibrationMeasurement m = new CalibrationMeasurement(param, nominal, measured, unit, tol);
                        record.addMeasurement(m);
                    } catch (Exception ignored) {}
                }
            }
        }
    }
}
