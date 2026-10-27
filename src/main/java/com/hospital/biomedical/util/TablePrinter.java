package com.hospital.biomedical.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Beautiful ASCII table formatter for clear console demonstration.
 */
public final class TablePrinter {
    private TablePrinter() {}

    public static void printTable(String title, String[] headers, List<String[]> rows) {
        if (headers == null || headers.length == 0) return;

        int cols = headers.length;
        int[] colWidths = new int[cols];

        for (int i = 0; i < cols; i++) {
            colWidths[i] = headers[i].length();
        }

        if (rows != null) {
            for (String[] row : rows) {
                for (int i = 0; i < cols && i < row.length; i++) {
                    String val = row[i] != null ? row[i] : "";
                    if (val.length() > colWidths[i]) {
                        colWidths[i] = Math.min(val.length(), 40); // Cap max column width to prevent overflow
                    }
                }
            }
        }

        // Add padding
        for (int i = 0; i < cols; i++) {
            colWidths[i] += 2;
        }

        int totalWidth = Arrays.stream(colWidths).sum() + cols + 1;

        if (title != null && !title.trim().isEmpty()) {
            System.out.println();
            System.out.println("=".repeat(Math.max(totalWidth, title.length() + 4)));
            System.out.println("  " + title);
            System.out.println("=".repeat(Math.max(totalWidth, title.length() + 4)));
        }

        printSeparator(colWidths);
        printRow(headers, colWidths);
        printSeparator(colWidths);

        if (rows == null || rows.isEmpty()) {
            System.out.println("| " + centerText("No records found", totalWidth - 4) + " |");
        } else {
            for (String[] row : rows) {
                printRow(row, colWidths);
            }
        }
        printSeparator(colWidths);
        System.out.println();
    }

    private static void printSeparator(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) {
            sb.append("-".repeat(w)).append("+");
        }
        System.out.println(sb);
    }

    private static void printRow(String[] items, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < widths.length; i++) {
            String val = (i < items.length && items[i] != null) ? items[i] : "";
            if (val.length() > widths[i] - 2) {
                val = val.substring(0, widths[i] - 5) + "...";
            }
            int padding = widths[i] - val.length() - 1;
            sb.append(" ").append(val).append(" ".repeat(Math.max(0, padding))).append("|");
        }
        System.out.println(sb);
    }

    private static String centerText(String text, int width) {
        if (text.length() >= width) return text.substring(0, width);
        int pad = (width - text.length()) / 2;
        return " ".repeat(pad) + text + " ".repeat(width - text.length() - pad);
    }
}
