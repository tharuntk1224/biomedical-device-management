package com.hospital.biomedical.model.user;

import com.hospital.biomedical.model.common.Address;
import com.hospital.biomedical.model.enums.UserRole;

/**
 * Specialized Technician entity extending User.
 * Demonstrates Inheritance and Polymorphic designation.
 */
public class Technician extends User {
    private String certificationNumber;
    private String specialization;
    private int yearsOfExperience;
    private boolean available;

    public Technician() {
        super();
        setRole(UserRole.TECHNICIAN);
        this.available = true;
    }

    public Technician(String id, String username, String passwordHash, String salt,
                      String fullName, String email, String phone, String department,
                      Address address, String certificationNumber, String specialization,
                      int yearsOfExperience) {
        super(id, username, passwordHash, salt, UserRole.TECHNICIAN, fullName, email, phone, department, address);
        setCertificationNumber(certificationNumber);
        setSpecialization(specialization);
        setYearsOfExperience(yearsOfExperience);
        this.available = true;
    }

    public String getCertificationNumber() {
        return certificationNumber;
    }

    public void setCertificationNumber(String certificationNumber) {
        if (certificationNumber == null || certificationNumber.trim().isEmpty()) {
            this.certificationNumber = "BMET-CERT-PENDING";
        } else {
            this.certificationNumber = certificationNumber.trim().toUpperCase();
        }
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization != null ? specialization.trim() : "General Biomedical";
    }

    public int getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(int yearsOfExperience) {
        if (yearsOfExperience < 0) {
            throw new IllegalArgumentException("Years of experience cannot be negative.");
        }
        this.yearsOfExperience = yearsOfExperience;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String getDesignation() {
        return String.format("Certified BMET [%s] - Specialization: %s (%d yrs exp)",
                certificationNumber, specialization, yearsOfExperience);
    }

    @Override
    public String toAuditSummary() {
        return String.format("Technician: %s (Cert: %s, Spec: %s, Available: %s)",
                getFullName(), certificationNumber, specialization, available);
    }
}
