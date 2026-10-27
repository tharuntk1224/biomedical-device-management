package com.hospital.biomedical.model.calibration;

import java.io.Serializable;
import java.util.Objects;

/**
 * Individual physical measurement taken during a calibration verification session.
 * Encapsulates tolerance evaluation and deviation formulas.
 */
public class CalibrationMeasurement implements Serializable {
    private String parameterName;
    private double nominalValue;
    private double measuredValue;
    private String unit;
    private double allowedTolerance;
    private double deviation;
    private boolean passed;

    public CalibrationMeasurement() {
    }

    public CalibrationMeasurement(String parameterName, double nominalValue, double measuredValue,
                                  String unit, double allowedTolerance) {
        setParameterName(parameterName);
        this.nominalValue = nominalValue;
        this.measuredValue = measuredValue;
        setUnit(unit);
        setAllowedTolerance(allowedTolerance);
        recalculate();
    }

    public final void recalculate() {
        this.deviation = Math.round(Math.abs(this.measuredValue - this.nominalValue) * 1000.0) / 1000.0;
        this.passed = this.deviation <= this.allowedTolerance;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        if (parameterName == null || parameterName.trim().isEmpty()) {
            throw new IllegalArgumentException("Measurement parameter name cannot be blank.");
        }
        this.parameterName = parameterName.trim();
    }

    public double getNominalValue() {
        return nominalValue;
    }

    public void setNominalValue(double nominalValue) {
        this.nominalValue = nominalValue;
        recalculate();
    }

    public double getMeasuredValue() {
        return measuredValue;
    }

    public void setMeasuredValue(double measuredValue) {
        this.measuredValue = measuredValue;
        recalculate();
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = (unit != null && !unit.trim().isEmpty()) ? unit.trim() : "units";
    }

    public double getAllowedTolerance() {
        return allowedTolerance;
    }

    public void setAllowedTolerance(double allowedTolerance) {
        if (allowedTolerance < 0) {
            throw new IllegalArgumentException("Allowed tolerance cannot be negative.");
        }
        this.allowedTolerance = allowedTolerance;
        recalculate();
    }

    public double getDeviation() {
        return deviation;
    }

    public boolean isPassed() {
        return passed;
    }

    @Override
    public String toString() {
        return String.format("%s: Nominal=%.3f %s, Measured=%.3f %s, Dev=%.3f (Tol=±%.3f) -> [%s]",
                parameterName, nominalValue, unit, measuredValue, unit, deviation, allowedTolerance,
                passed ? "PASS" : "FAIL");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CalibrationMeasurement that)) return false;
        return Double.compare(that.nominalValue, nominalValue) == 0 &&
               Double.compare(that.measuredValue, measuredValue) == 0 &&
               Double.compare(that.allowedTolerance, allowedTolerance) == 0 &&
               Objects.equals(parameterName, that.parameterName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameterName, nominalValue, measuredValue, allowedTolerance);
    }
}
