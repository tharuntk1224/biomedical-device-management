package com.hospital.biomedical.repository;

import com.hospital.biomedical.model.common.Address;
import com.hospital.biomedical.model.enums.UserRole;
import com.hospital.biomedical.model.user.Technician;
import com.hospital.biomedical.model.user.User;
import com.hospital.biomedical.util.CsvUtils;
import com.hospital.biomedical.util.SecurityUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data access and persistence repository for hospital users and technicians.
 */
public class UserRepository {
    private final Map<String, User> usersByUsername = new LinkedHashMap<>();
    private final Map<String, User> usersById = new HashMap<>();
    private final Path storagePath;

    public UserRepository() {
        this(Paths.get("data", "users.csv"));
    }

    public UserRepository(Path storagePath) {
        this.storagePath = storagePath;
        load();
        if (usersByUsername.isEmpty()) {
            seedDefaultAccounts();
        }
    }

    public synchronized void save(User user) {
        Objects.requireNonNull(user, "User cannot be null.");
        usersByUsername.put(user.getUsername().toLowerCase(), user);
        usersById.put(user.getId(), user);
        flush();
    }

    public synchronized Optional<User> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return Optional.ofNullable(usersByUsername.get(username.trim().toLowerCase()));
    }

    public synchronized Optional<User> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(usersById.get(id));
    }

    public synchronized List<User> findAll() {
        return new ArrayList<>(usersByUsername.values());
    }

    public synchronized List<Technician> findAllTechnicians() {
        return usersByUsername.values().stream()
                .filter(u -> u instanceof Technician)
                .map(u -> (Technician) u)
                .collect(Collectors.toList());
    }

    public synchronized Optional<Technician> findTechnicianById(String id) {
        return findById(id).filter(u -> u instanceof Technician).map(u -> (Technician) u);
    }

    public synchronized boolean existsByUsername(String username) {
        return username != null && usersByUsername.containsKey(username.trim().toLowerCase());
    }

    public synchronized void clear() {
        usersByUsername.clear();
        usersById.clear();
        flush();
    }

    public synchronized void resetToDefaultAccounts() {
        clear();
        seedDefaultAccounts();
    }

    private synchronized void seedDefaultAccounts() {
        // Seed default demonstration credentials with securely salted hashes
        createAndSeedUser("USR-001", "admin", "Admin@123", UserRole.ADMIN,
                "Dr. Eleanor Vance", "admin@hospital.org", "555-0100", "Hospital Administration");

        createAndSeedTechnician("TECH-101", "tech1", "Tech@123",
                "Marcus Rivera", "tech1@hospital.org", "555-0201", "Biomedical Engineering",
                "CBET-2024-991", "Life-Support Systems", 7);

        createAndSeedTechnician("TECH-102", "tech2", "Tech@123",
                "Dr. Sarah Lin", "tech2@hospital.org", "555-0202", "Diagnostic Physics",
                "CBET-2023-412", "Diagnostic & Radiation Equipment", 10);

        createAndSeedUser("USR-002", "staff1", "Staff@123", UserRole.STAFF,
                "Nurse Jennifer Blake", "nurse.blake@hospital.org", "555-0301", "Intensive Care Unit (ICU)");

        createAndSeedUser("USR-003", "auditor1", "Audit@123", UserRole.AUDITOR,
                "Inspector Arthur Dent", "auditor@hospital.org", "555-0401", "Clinical Quality Assurance");
    }

    private void createAndSeedUser(String id, String username, String rawPassword, UserRole role,
                                   String fullName, String email, String phone, String department) {
        String salt = SecurityUtils.generateSalt();
        String hash = SecurityUtils.hashPassword(rawPassword, salt);
        User user = new User(id, username, hash, salt, role, fullName, email, phone, department,
                new Address("Main Hospital", "3", "Admin Suite 302", "North Wing"));
        usersByUsername.put(user.getUsername().toLowerCase(), user);
        usersById.put(user.getId(), user);
        flush();
    }

    private void createAndSeedTechnician(String id, String username, String rawPassword,
                                         String fullName, String email, String phone, String department,
                                         String certNo, String spec, int exp) {
        String salt = SecurityUtils.generateSalt();
        String hash = SecurityUtils.hashPassword(rawPassword, salt);
        Technician tech = new Technician(id, username, hash, salt, fullName, email, phone, department,
                new Address("Engineering Annex", "B1", "Bio-Shop 04", "East Wing"), certNo, spec, exp);
        usersByUsername.put(tech.getUsername().toLowerCase(), tech);
        usersById.put(tech.getId(), tech);
        flush();
    }

    private synchronized void flush() {
        List<String> lines = new ArrayList<>();
        lines.add(CsvUtils.toCsvLine("type", "id", "username", "passwordHash", "salt", "role",
                "fullName", "email", "phone", "department", "active", "certNo", "specialization", "yearsExp", "available"));

        for (User u : usersByUsername.values()) {
            boolean isTech = (u instanceof Technician);
            Technician t = isTech ? (Technician) u : null;
            lines.add(CsvUtils.toCsvLine(
                    isTech ? "Technician" : "User",
                    u.getId(),
                    u.getUsername(),
                    u.getPasswordHash(),
                    u.getSalt(),
                    u.getRole().name(),
                    u.getFullName(),
                    u.getEmail(),
                    u.getPhone(),
                    u.getDepartment(),
                    String.valueOf(u.isActive()),
                    isTech ? t.getCertificationNumber() : "",
                    isTech ? t.getSpecialization() : "",
                    isTech ? String.valueOf(t.getYearsOfExperience()) : "",
                    isTech ? String.valueOf(t.isAvailable()) : ""
            ));
        }

        try {
            CsvUtils.writeLinesAtomic(storagePath, lines);
        } catch (IOException e) {
            System.err.println("Error saving users file: " + e.getMessage());
        }
    }

    private synchronized void load() {
        usersByUsername.clear();
        usersById.clear();
        List<String> lines = CsvUtils.readLinesSafe(storagePath);
        if (lines.isEmpty()) return;

        boolean isFirst = true;
        for (String line : lines) {
            if (isFirst) {
                isFirst = false;
                continue;
            }
            List<String> cols = CsvUtils.parseCsvLine(line);
            if (cols.size() < 11) continue;

            try {
                String type = cols.get(0);
                String id = cols.get(1);
                String username = cols.get(2);
                String hash = cols.get(3);
                String salt = cols.get(4);
                UserRole role = UserRole.valueOf(cols.get(5));
                String fullName = cols.get(6);
                String email = cols.get(7);
                String phone = cols.get(8);
                String department = cols.get(9);
                boolean active = Boolean.parseBoolean(cols.get(10));

                User user;
                if ("Technician".equals(type) && cols.size() >= 15) {
                    String cert = cols.get(11);
                    String spec = cols.get(12);
                    int exp = Integer.parseInt(cols.get(13));
                    boolean avail = Boolean.parseBoolean(cols.get(14));
                    Technician tech = new Technician(id, username, hash, salt, fullName, email, phone,
                            department, new Address(), cert, spec, exp);
                    tech.setAvailable(avail);
                    user = tech;
                } else {
                    user = new User(id, username, hash, salt, role, fullName, email, phone, department, new Address());
                }
                user.setActive(active);

                usersByUsername.put(user.getUsername().toLowerCase(), user);
                usersById.put(user.getId(), user);
            } catch (Exception e) {
                System.err.println("Warning: Corrupt user row skipped: " + e.getMessage());
            }
        }
    }
}
