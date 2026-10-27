package com.hospital.biomedical.exception;

public class CalibrationToleranceExceededException extends Exception {
    private final String parameterName;
    private final double nominalValue;
    private final double measuredValue;
    private final double deviation;
    private final double allowedTolerance;

    public CalibrationToleranceExceededException(String parameterName, double nominalValue, double measuredValue, double deviation, double allowedTolerance) {
        super(String.format("Calibration tolerance exceeded for '%s': measured=%.3f, nominal=%.3f, deviation=%.3f > allowed=%.3f",
                parameterName, measuredValue, nominalValue, deviation, allowedTolerance));
        this.parameterName = parameterName;
        this.nominalValue = nominalValue;
        this.measuredValue = measuredValue;
        this.deviation = deviation;
        this.allowedTolerance = allowedTolerance;
    }

    public String getParameterName() {
        return parameterName;
    }

    public double getNominalValue() {
        return nominalValue;
    }

    public double getMeasuredValue() {
        return measuredValue;
    }

    public double getDeviation() {
        return deviation;
    }

    public double getAllowedTolerance() {
        return allowedTolerance;
    }
}
