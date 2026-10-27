package com.hospital.biomedical.model.common;

/**
 * Abstract Person entity demonstrating Inheritance from BaseEntity.
 */
public abstract class Person extends BaseEntity {
    private String fullName;
    private String email;
    private String phone;
    private String department;
    private Address address;

    public Person() {
        super();
        this.address = new Address();
    }

    public Person(String id, String fullName, String email, String phone, String department, Address address) {
        super(id);
        setFullName(fullName);
        setEmail(email);
        setPhone(phone);
        this.department = department != null ? department.trim() : "Biomedical Engineering";
        this.address = address != null ? address : new Address();
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Person full name cannot be null or empty.");
        }
        this.fullName = fullName.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("A valid email address is required.");
        }
        this.email = email.trim().toLowerCase();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = (phone != null && !phone.trim().isEmpty()) ? phone.trim() : "N/A";
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department != null ? department.trim() : "General";
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address != null ? address : new Address();
    }

    /**
     * Polymorphic method to obtain a role-specific designation string.
     */
    public abstract String getDesignation();
}
