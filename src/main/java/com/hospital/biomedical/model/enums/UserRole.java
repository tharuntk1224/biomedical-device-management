package com.hospital.biomedical.model.enums;

/**
 * Access control roles within the biomedical engineering ecosystem.
 */
public enum UserRole {
    ADMIN("Hospital Biomedical Administrator with full operational governance"),
    TECHNICIAN("Certified Biomedical Equipment Technician (BMET)"),
    STAFF("Clinical or Nursing Staff with read and request permissions"),
    AUDITOR("Compliance and Quality Assurance Auditor");

    private final String title;

    UserRole(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
