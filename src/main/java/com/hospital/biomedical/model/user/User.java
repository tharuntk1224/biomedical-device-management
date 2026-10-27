package com.hospital.biomedical.model.user;

import com.hospital.biomedical.interfaces.Auditable;
import com.hospital.biomedical.model.common.Address;
import com.hospital.biomedical.model.common.Person;
import com.hospital.biomedical.model.enums.UserRole;

/**
 * System user entity extending Person.
 * Implements Auditable.
 */
public class User extends Person implements Auditable {
    private String username;
    private String passwordHash;
    private String salt;
    private UserRole role;
    private boolean active;

    public User() {
        super();
        this.active = true;
        this.role = UserRole.STAFF;
    }

    public User(String id, String username, String passwordHash, String salt,
                UserRole role, String fullName, String email, String phone,
                String department, Address address) {
        super(id, fullName, email, phone, department, address);
        setUsername(username);
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.role = role != null ? role : UserRole.STAFF;
        this.active = true;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }
        this.username = username.trim().toLowerCase();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null.");
        }
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String getDesignation() {
        return role.name() + " (" + role.getTitle() + ")";
    }

    @Override
    public String getEntityIdentifier() {
        return getId();
    }

    @Override
    public String getEntityTypeName() {
        return "User";
    }

    @Override
    public String toAuditSummary() {
        return String.format("User: %s [%s], Role: %s, Department: %s",
                username, getFullName(), role, getDepartment());
    }

    @Override
    public String toString() {
        return String.format("%s (%s - %s)", getFullName(), username, role);
    }
}
