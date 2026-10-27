package com.hospital.biomedical.repository;

import com.hospital.biomedical.exception.DuplicateEntityException;
import com.hospital.biomedical.model.device.*;
import com.hospital.biomedical.model.enums.CalibrationDueStatus;
import com.hospital.biomedical.model.enums.DeviceStatus;
import com.hospital.biomedical.model.enums.RiskLevel;
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
 * Data access and persistence repository for BiomedicalDevice entities.
 * Supports overloaded search methods demonstrating Polymorphism.
 */
public class DeviceRepository {
    private final Map<String, BiomedicalDevice> devicesById = new LinkedHashMap<>();
    private final Map<String, String> assetNumberToId = new HashMap<>();
    private final Path storagePath;

    public DeviceRepository() {
        this(Paths.get("data", "devices.csv"));
    }

    public DeviceRepository(Path storagePath) {
        this.storagePath = storagePath;
        load();
    }

    public synchronized void save(BiomedicalDevice device) {
        Objects.requireNonNull(device, "Device cannot be null.");
        String existingIdWithAsset = assetNumberToId.get(device.getAssetNumber());
        if (existingIdWithAsset != null && !existingIdWithAsset.equalsIgnoreCase(device.getId())) {
            throw new DuplicateEntityException("A device with Asset Number '" + device.getAssetNumber() + "' already exists (ID: " + existingIdWithAsset + ").");
        }

        devicesById.put(device.getId(), device);
        assetNumberToId.put(device.getAssetNumber(), device.getId());
        flush();
    }

    public synchronized Optional<BiomedicalDevice> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(devicesById.get(id.trim().toUpperCase()));
    }

    public synchronized Optional<BiomedicalDevice> findByAssetNumber(String assetNumber) {
        if (assetNumber == null) return Optional.empty();
        String id = assetNumberToId.get(assetNumber.trim().toUpperCase());
        return id != null ? Optional.ofNullable(devicesById.get(id)) : Optional.empty();
    }

    // Overloaded search methods (Polymorphism)
    public synchronized List<BiomedicalDevice> findDevices() {
        return new ArrayList<>(devicesById.values());
    }

    public synchronized List<BiomedicalDevice> findDevices(String department) {
        if (department == null || department.trim().isEmpty() || "ALL".equalsIgnoreCase(department)) {
            return findDevices();
        }
        String deptLower = department.trim().toLowerCase();
        return devicesById.values().stream()
                .filter(d -> d.getDepartment().toLowerCase().contains(deptLower))
                .collect(Collectors.toList());
    }

    public synchronized List<BiomedicalDevice> findDevices(CalibrationDueStatus status, LocalDate referenceDate) {
        Objects.requireNonNull(status, "Status cannot be null.");
        LocalDate ref = referenceDate != null ? referenceDate : LocalDate.now();
        return devicesById.values().stream()
                .filter(d -> d.getCalibrationDueStatus(ref) == status)
                .collect(Collectors.toList());
    }

    public synchronized List<BiomedicalDevice> findDevices(DeviceStatus status) {
        Objects.requireNonNull(status, "DeviceStatus cannot be null.");
        return devicesById.values().stream()
                .filter(d -> d.getDeviceStatus() == status)
                .collect(Collectors.toList());
    }

    public synchronized boolean existsById(String id) {
        return id != null && devicesById.containsKey(id.trim().toUpperCase());
    }

    public synchronized boolean existsByAssetNumber(String assetNumber) {
        return assetNumber != null && assetNumberToId.containsKey(assetNumber.trim().toUpperCase());
    }

    public synchronized int count() {
        return devicesById.size();
    }

    public synchronized void clear() {
        devicesById.clear();
        assetNumberToId.clear();
        flush();
    }

    private synchronized void flush() {
        List<String> lines = new ArrayList<>();
        // CSV Header
        lines.add(CsvUtils.toCsvLine("type", "id", "assetNumber", "serialNumber", "name",
                "manufacturer", "modelNumber", "department", "category", "riskLevel",
                "status", "calibRequired", "frequencyDays", "lastCalib", "nextCalib", "assignedTech", "extraParams"));

        for (BiomedicalDevice d : devicesById.values()) {
            String type = d.getClass().getSimpleName();
            String extraParams = "";

            if (d instanceof LifeSupportDevice lsd) {
                extraParams = lsd.getBatteryBackupMinutes() + ";" + lsd.isRedundantAlarmsOperational();
            } else if (d instanceof DiagnosticDevice dd) {
                extraParams = dd.isRadiationEmitting() + ";" + dd.getSoftwareFirmwareVersion();
            } else if (d instanceof LaboratoryDevice ld) {
                extraParams = ld.isRequiresTemperatureControl() + ";" + ld.getOperatingTemperatureCelsius() + ";" + ld.getBiosafetyLevel();
            }

            lines.add(CsvUtils.toCsvLine(
                    type,
                    d.getId(),
                    d.getAssetNumber(),
                    d.getSerialNumber(),
                    d.getName(),
                    d.getManufacturer(),
                    d.getModelNumber(),
                    d.getDepartment(),
                    d.getCategory().name(),
                    d.getRiskLevel().name(),
                    d.getDeviceStatus().name(),
                    String.valueOf(d.isCalibrationRequired()),
                    String.valueOf(d.getCalibrationFrequencyDays()),
                    DateUtils.formatDate(d.getLastCalibrationDate()),
                    DateUtils.formatDate(d.getNextCalibrationDate()),
                    d.getAssignedTechnicianId(),
                    extraParams
            ));
        }

        try {
            CsvUtils.writeLinesAtomic(storagePath, lines);
        } catch (IOException e) {
            System.err.println("Error saving device records: " + e.getMessage());
        }
    }

    private synchronized void load() {
        devicesById.clear();
        assetNumberToId.clear();
        List<String> lines = CsvUtils.readLinesSafe(storagePath);
        if (lines.isEmpty()) return;

        boolean isFirst = true;
        for (String line : lines) {
            if (isFirst) {
                isFirst = false;
                continue;
            }
            List<String> cols = CsvUtils.parseCsvLine(line);
            if (cols.size() < 16) continue;

            try {
                String type = cols.get(0);
                String id = cols.get(1);
                String assetNumber = cols.get(2);
                String serialNumber = cols.get(3);
                String name = cols.get(4);
                String manufacturer = cols.get(5);
                String modelNumber = cols.get(6);
                String department = cols.get(7);
                DeviceCategory category = DeviceCategory.valueOf(cols.get(8));
                RiskLevel riskLevel = RiskLevel.valueOf(cols.get(9));
                DeviceStatus status = DeviceStatus.valueOf(cols.get(10));
                boolean calibRequired = Boolean.parseBoolean(cols.get(11));
                int freq = Integer.parseInt(cols.get(12));
                LocalDate lastCalib = DateUtils.parseDate(cols.get(13));
                LocalDate nextCalib = DateUtils.parseDate(cols.get(14));
                String assignedTech = cols.get(15);
                String extra = cols.size() > 16 ? cols.get(16) : "";

                BiomedicalDevice device;
                if ("LifeSupportDevice".equals(type)) {
                    int battery = 120;
                    boolean alarms = true;
                    if (!extra.isEmpty()) {
                        String[] parts = extra.split(";");
                        if (parts.length >= 2) {
                            battery = Integer.parseInt(parts[0]);
                            alarms = Boolean.parseBoolean(parts[1]);
                        }
                    }
                    LifeSupportDevice lsd = new LifeSupportDevice(id, assetNumber, serialNumber, name,
                            manufacturer, modelNumber, department, freq, battery, alarms);
                    device = lsd;
                } else if ("DiagnosticDevice".equals(type)) {
                    boolean radiation = false;
                    String fw = "v1.0";
                    if (!extra.isEmpty()) {
                        String[] parts = extra.split(";");
                        if (parts.length >= 2) {
                            radiation = Boolean.parseBoolean(parts[0]);
                            fw = parts[1];
                        }
                    }
                    DiagnosticDevice dd = new DiagnosticDevice(id, assetNumber, serialNumber, name,
                            manufacturer, modelNumber, department, riskLevel, freq, radiation, fw);
                    device = dd;
                } else if ("LaboratoryDevice".equals(type)) {
                    boolean tempCtrl = false;
                    double temp = 22.0;
                    int bsl = 1;
                    if (!extra.isEmpty()) {
                        String[] parts = extra.split(";");
                        if (parts.length >= 3) {
                            tempCtrl = Boolean.parseBoolean(parts[0]);
                            temp = Double.parseDouble(parts[1]);
                            bsl = Integer.parseInt(parts[2]);
                        }
                    }
                    LaboratoryDevice ld = new LaboratoryDevice(id, assetNumber, serialNumber, name,
                            manufacturer, modelNumber, department, riskLevel, freq, tempCtrl, temp, bsl);
                    device = ld;
                } else {
                    // Fallback to DiagnosticDevice
                    device = new DiagnosticDevice(id, assetNumber, serialNumber, name, manufacturer,
                            modelNumber, department, riskLevel, freq, false, "v1.0");
                }

                device.setDeviceStatus(status);
                device.setLastCalibrationDate(lastCalib);
                device.setNextCalibrationDate(nextCalib);
                device.setAssignedTechnicianId(assignedTech);

                devicesById.put(device.getId(), device);
                assetNumberToId.put(device.getAssetNumber(), device.getId());

                // Update ID sequence tracker
                try {
                    if (id.startsWith("DEV-")) {
                        long num = Long.parseLong(id.substring(4));
                        IdGenerator.updateDeviceSequence(num);
                    }
                } catch (Exception ignored) {}

            } catch (Exception e) {
                System.err.println("Warning: Corrupt device record skipped: " + e.getMessage());
            }
        }
    }
}
