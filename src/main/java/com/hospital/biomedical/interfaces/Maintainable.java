package com.hospital.biomedical.interfaces;

import com.hospital.biomedical.model.enums.DeviceStatus;

/**
 * Interface defining contract for equipment requiring preventive or corrective maintenance.
 */
public interface Maintainable {
    void startMaintenance();
    void completeMaintenance(boolean requireRecalibration);
    DeviceStatus getDeviceStatus();
    boolean canBeOperated();
}
