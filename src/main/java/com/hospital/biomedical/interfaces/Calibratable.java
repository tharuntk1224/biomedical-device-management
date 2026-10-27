package com.hospital.biomedical.interfaces;

import com.hospital.biomedical.model.enums.CalibrationDueStatus;
import java.time.LocalDate;

/**
 * Interface defining contract for equipment requiring periodic calibration.
 */
public interface Calibratable {
    boolean isCalibrationRequired();
    int getCalibrationFrequencyDays();
    LocalDate getLastCalibrationDate();
    LocalDate getNextCalibrationDate();
    void recordCalibration(LocalDate calibrationDate, boolean passed);
    CalibrationDueStatus getCalibrationDueStatus(LocalDate referenceDate);
}
