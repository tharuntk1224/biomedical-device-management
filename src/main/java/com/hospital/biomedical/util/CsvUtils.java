package com.hospital.biomedical.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Robust CSV serialization utility with proper RFC-4180 style quoting and atomic file replacement.
 */
public final class CsvUtils {
    private CsvUtils() {}

    public static String escapeField(String val) {
        if (val == null) {
            return "";
        }
        if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    public static String toCsvLine(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(escapeField(fields[i]));
        }
        return sb.toString();
    }

    public static List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        if (line == null || line.trim().isEmpty()) {
            return result;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;
        int len = line.length();

        while (i < len) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < len && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    result.add(current.toString().trim());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
            i++;
        }
        result.add(current.toString().trim());
        return result;
    }

    public static void writeLinesAtomic(Path targetPath, List<String> lines) throws IOException {
        Path parent = targetPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        Path tempPath = targetPath.resolveSibling(targetPath.getFileName().toString() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }

        try {
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static List<String> readLinesSafe(Path path) {
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Warning: Unable to read file " + path + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
