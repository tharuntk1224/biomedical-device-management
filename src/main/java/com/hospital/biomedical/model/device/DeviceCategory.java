package com.hospital.biomedical.model.device;

/**
 * Functional classification of biomedical devices.
 */
public enum DeviceCategory {
    LIFE_SUPPORT("Sustains essential bodily functions (e.g. Ventilator, Defibrillator, Infusion Pump)"),
    DIAGNOSTIC("Imaging, monitoring, and detection (e.g. ECG Monitor, Ultrasound, X-Ray)"),
    LABORATORY("In-vitro and analytical instruments (e.g. Centrifuge, Blood Gas Analyzer)"),
    THERAPEUTIC("Rehabilitative or therapeutic application");

    private final String description;

    DeviceCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
