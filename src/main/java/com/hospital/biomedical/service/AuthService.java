package com.hospital.biomedical.service;

import com.hospital.biomedical.exception.UnauthorizedAccessException;
import com.hospital.biomedical.model.enums.UserRole;
import com.hospital.biomedical.model.user.User;
import com.hospital.biomedical.repository.UserRepository;
import com.hospital.biomedical.util.SecurityUtils;

import java.util.Arrays;
import java.util.Optional;

/**
 * Authentication and authorization service with session context and role enforcement.
 */
public class AuthService {
    private final UserRepository userRepository;
    private final AuditService auditService;
    private User currentUser;

    public AuthService(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        // Default session is null until explicit login
    }

    public synchronized User login(String identifier, String password) {
        if (identifier == null || password == null) {
            throw new IllegalArgumentException("Username/email and password are required.");
        }

        String clean = identifier.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByUsername(clean);

        if (userOpt.isEmpty()) {
            // Check if matches email or username prefix before @
            String userPrefix = clean.contains("@") ? clean.split("@")[0] : clean;
            userOpt = userRepository.findAll().stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(clean) ||
                                 u.getUsername().equalsIgnoreCase(userPrefix))
                    .findFirst();
        }

        if (userOpt.isEmpty()) {
            auditService.log("ANONYMOUS", "LOGIN_FAILED", "User", identifier, "Identifier not found");
            throw new UnauthorizedAccessException("Invalid operator credentials.");
        }

        User user = userOpt.get();
        if (!user.isActive()) {
            auditService.log(user.getUsername(), "LOGIN_BLOCKED", "User", user.getId(), "Account is inactive");
            throw new UnauthorizedAccessException("Account is disabled. Contact system administrator.");
        }

        boolean matched = SecurityUtils.verifyPassword(password, user.getSalt(), user.getPasswordHash());
        if (!matched) {
            auditService.log(user.getUsername(), "LOGIN_FAILED", "User", user.getId(), "Password hash mismatch");
            throw new UnauthorizedAccessException("Invalid credentials provided.");
        }

        this.currentUser = user;
        auditService.log(user.getUsername(), "LOGIN_SUCCESS", "User", user.getId(), "User logged in with role: " + user.getRole());
        return user;
    }

    public synchronized User registerUser(String username, String password, String fullName,
                                         String email, String department, UserRole role) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }
        String cleanUser = username.trim().toLowerCase();
        if (userRepository.existsByUsername(cleanUser)) {
            throw new IllegalArgumentException("Operator username '" + cleanUser + "' is already registered.");
        }

        String salt = SecurityUtils.generateSalt();
        String hash = SecurityUtils.hashPassword(password, salt);
        String id = "USR-" + (System.currentTimeMillis() % 100000);

        String cleanEmail = (email != null && !email.trim().isEmpty()) ? email.trim() : cleanUser + "@hospital.org";
        String cleanDept = (department != null && !department.trim().isEmpty()) ? department.trim() : "Biomedical Engineering";
        String cleanName = (fullName != null && !fullName.trim().isEmpty()) ? fullName.trim() : cleanUser;
        UserRole cleanRole = role != null ? role : UserRole.TECHNICIAN;

        User newUser = new User(id, cleanUser, hash, salt, cleanRole, cleanName, cleanEmail, "N/A", cleanDept,
                new com.hospital.biomedical.model.common.Address());
        userRepository.save(newUser);
        auditService.log(cleanUser, "USER_REGISTERED", "User", id, "New operator registered: " + cleanRole);
        this.currentUser = newUser;
        return newUser;
    }

    public synchronized void logout() {
        if (currentUser != null) {
            auditService.log(currentUser.getUsername(), "LOGOUT", "User", currentUser.getId(), "Session ended");
            currentUser = null;
        }
    }

    public synchronized User getCurrentUser() {
        return currentUser;
    }

    public synchronized void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Enforces required roles; throws UnauthorizedAccessException if unauthorized.
     */
    public synchronized void checkPermission(UserRole... allowedRoles) {
        if (currentUser == null) {
            throw new UnauthorizedAccessException("Authentication required. Please log in first.");
        }

        for (UserRole role : allowedRoles) {
            if (currentUser.getRole() == role) {
                return; // Authorized
            }
        }

        auditService.log(currentUser.getUsername(), "ACCESS_DENIED", "Security", "ROLE_CHECK",
                "User role " + currentUser.getRole() + " attempted access requiring: " + Arrays.toString(allowedRoles));
        throw new UnauthorizedAccessException("Access denied. Role required: " + Arrays.toString(allowedRoles)
                + " (current role: " + currentUser.getRole() + ")");
    }
}
