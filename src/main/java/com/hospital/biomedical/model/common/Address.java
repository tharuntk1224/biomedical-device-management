package com.hospital.biomedical.model.common;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value object encapsulating physical location details within hospital premises.
 */
public class Address implements Serializable {
    private String building;
    private String floor;
    private String roomNumber;
    private String wing;

    public Address() {
        this("Main Hospital", "1", "General", "Central Wing");
    }

    public Address(String building, String floor, String roomNumber, String wing) {
        this.building = building != null ? building.trim() : "Main Hospital";
        this.floor = floor != null ? floor.trim() : "1";
        this.roomNumber = roomNumber != null ? roomNumber.trim() : "General";
        this.wing = wing != null ? wing.trim() : "Central Wing";
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getWing() {
        return wing;
    }

    public void setWing(String wing) {
        this.wing = wing;
    }

    public String getFullLocation() {
        return String.format("%s, Floor %s, %s (Room %s)", building, floor, wing, roomNumber);
    }

    @Override
    public String toString() {
        return getFullLocation();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address address)) return false;
        return Objects.equals(building, address.building) &&
               Objects.equals(floor, address.floor) &&
               Objects.equals(roomNumber, address.roomNumber) &&
               Objects.equals(wing, address.wing);
    }

    @Override
    public int hashCode() {
        return Objects.hash(building, floor, roomNumber, wing);
    }
}
