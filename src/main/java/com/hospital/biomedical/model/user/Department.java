package com.hospital.biomedical.model.user;

import com.hospital.biomedical.model.common.BaseEntity;

/**
 * Hospital department entity.
 */
public class Department extends BaseEntity {
    private String name;
    private String code;
    private String floorLocation;
    private String headOfDepartment;

    public Department() {
        super();
    }

    public Department(String id, String name, String code, String floorLocation, String headOfDepartment) {
        super(id);
        setName(name);
        setCode(code);
        this.floorLocation = floorLocation != null ? floorLocation : "Level 1";
        this.headOfDepartment = headOfDepartment != null ? headOfDepartment : "Chief Medical Officer";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Department name cannot be blank.");
        }
        this.name = name.trim();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Department code cannot be blank.");
        }
        this.code = code.trim().toUpperCase();
    }

    public String getFloorLocation() {
        return floorLocation;
    }

    public void setFloorLocation(String floorLocation) {
        this.floorLocation = floorLocation;
    }

    public String getHeadOfDepartment() {
        return headOfDepartment;
    }

    public void setHeadOfDepartment(String headOfDepartment) {
        this.headOfDepartment = headOfDepartment;
    }

    @Override
    public String toString() {
        return name + " (" + code + ")";
    }
}
