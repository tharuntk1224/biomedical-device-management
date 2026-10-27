package com.hospital.biomedical.model.enums;

/**
 * Risk classification for biomedical equipment based on potential patient harm.
 */
public enum RiskLevel {
    LOW(1, "Minimal patient risk (e.g. scales, lab mixers)"),
    MEDIUM(2, "Moderate risk (e.g. diagnostic ultrasound, centrifuge)"),
    HIGH(3, "High risk (e.g. electrosurgical units, X-ray)"),
    CRITICAL(4, "Life-support or direct sustaining equipment (e.g. ventilators, defibrillators)");

    private final int severityScore;
    private final String description;

    RiskLevel(int severityScore, String description) {
        this.severityScore = severityScore;
        this.description = description;
    }

    public int getSeverityScore() {
        return severityScore;
    }

    public String getDescription() {
        return description;
    }
}
