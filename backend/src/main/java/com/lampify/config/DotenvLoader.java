package com.lampify.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads GOOGLE_CLIENT_ID and other values from a project .env file before Spring starts.
 */
public final class DotenvLoader {

    private DotenvLoader() {
    }

    public static void load() {
        int loaded = 0;

        for (Path envFile : findEnvFiles()) {
            loaded += loadEnvFile(envFile);
        }

        if (loaded > 0) {
            System.out.println("Loaded " + loaded + " variables from .env for local development");
        }
    }

    private static List<Path> findEnvFiles() {
        Set<Path> candidates = new LinkedHashSet<>();
        Path current = Path.of("").toAbsolutePath().normalize();

        for (int depth = 0; depth < 6 && current != null; depth++) {
            candidates.add(current.resolve(".env"));
            current = current.getParent();
        }

        return new ArrayList<>(candidates);
    }

    private static int loadEnvFile(Path path) {
        if (!Files.isRegularFile(path)) {
            return 0;
        }

        int loaded = 0;

        try {
            for (String line : Files.readAllLines(path)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int separator = line.indexOf('=');
                if (separator <= 0) {
                    continue;
                }

                String key = line.substring(0, separator).trim();
                String value = line.substring(separator + 1).trim();
                if (key.isEmpty() || hasConfiguredValue(key)) {
                    continue;
                }

                System.setProperty(key, value);
                loaded++;
            }
        } catch (Exception ignored) {
            // Optional dev convenience — ignore unreadable files.
        }

        return loaded;
    }

    private static boolean hasConfiguredValue(String key) {
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return true;
        }

        String propertyValue = System.getProperty(key);
        return propertyValue != null && !propertyValue.isBlank();
    }
}
