package com.hospital.biomedical.interfaces;

/**
 * Interface contract for models that can export structured reports.
 */
public interface Reportable {
    String[] getReportHeaders();
    String[] toReportRow();
}
