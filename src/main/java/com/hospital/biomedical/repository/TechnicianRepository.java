package com.hospital.biomedical.repository;

import com.hospital.biomedical.model.user.Technician;

import java.util.List;
import java.util.Optional;

/**
 * Repository layer specifically for technician operations, collaborating with UserRepository.
 */
public class TechnicianRepository {
    private final UserRepository userRepository;

    public TechnicianRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<Technician> findAll() {
        return userRepository.findAllTechnicians();
    }

    public Optional<Technician> findById(String id) {
        return userRepository.findTechnicianById(id);
    }

    public Optional<Technician> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .filter(u -> u instanceof Technician)
                .map(u -> (Technician) u);
    }

    public void save(Technician technician) {
        userRepository.save(technician);
    }
}
