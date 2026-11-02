package com.complaints.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class AppConfig {
    private final Properties values = new Properties();

    public AppConfig() throws IOException {
        Path file = findConfig();
        if (Files.exists(file)) {
            try (InputStream input = Files.newInputStream(file)) {
                values.load(input);
            }
        }
    }

    private static Path findConfig() throws IOException {
        List<Path> candidates = new ArrayList<>();
        candidates.add(Path.of("application.properties").toAbsolutePath());
        try {
            Path location = Path.of(AppConfig.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).toAbsolutePath();
            Path directory = Files.isDirectory(location) ? location : location.getParent();
            for (int i = 0; i < 3 && directory != null; i++, directory = directory.getParent()) {
                candidates.add(directory.resolve("application.properties"));
            }
        } catch (java.net.URISyntaxException e) {
            throw new IOException("Unable to locate the application configuration directory.", e);
        }
        return candidates.stream().filter(Files::isRegularFile).findFirst()
                .orElse(candidates.get(0));
    }

    public String get(String key, String fallback) {
        String envKey = "APP_" + key.toUpperCase().replace('.', '_');
        String fromEnvironment = System.getenv(envKey);
        if (fromEnvironment != null && !fromEnvironment.isBlank()) return fromEnvironment;
        return values.getProperty(key, fallback);
    }

    public boolean getBoolean(String key, boolean fallback) {
        return Boolean.parseBoolean(get(key, Boolean.toString(fallback)).trim());
    }
}
