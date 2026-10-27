package com.hospital.biomedical.web;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * WebFrontend loader for the Biomedical Equipment Management System SPA.
 * Loads the HTML/CSS/JS application from classpath resources or file paths.
 */
public final class WebFrontend {
    private WebFrontend() {}

    private static volatile String cachedHtml = null;

    public static String getHtmlPage() {
        if (cachedHtml != null) {
            return cachedHtml;
        }

        // 1. Try classpath resource
        try (InputStream is = WebFrontend.class.getResourceAsStream("/web/index.html")) {
            if (is != null) {
                cachedHtml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                return cachedHtml;
            }
        } catch (Exception ignored) {}

        // 2. Try file system relative locations
        String[] candidatePaths = {
            "web/index.html",
            "src/main/resources/web/index.html",
            "../web/index.html",
            "../src/main/resources/web/index.html"
        };

        for (String path : candidatePaths) {
            File f = new File(path);
            if (f.exists() && f.isFile()) {
                try {
                    cachedHtml = Files.readString(f.toPath(), StandardCharsets.UTF_8);
                    return cachedHtml;
                } catch (Exception ignored) {}
            }
        }

        return "<!DOCTYPE html><html><body><h2>Biomedical Registry: Error loading index.html resource</h2></body></html>";
    }

    /**
     * Clear cache in case live reloading during development is needed.
     */
    public static void clearCache() {
        cachedHtml = null;
    }
}
