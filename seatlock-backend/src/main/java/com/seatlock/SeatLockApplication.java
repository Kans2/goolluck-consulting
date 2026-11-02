package com.seatlock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * SeatLock — A production-grade concurrent seat booking system.
 *
 * <p>Uses pessimistic locking (SELECT … FOR UPDATE) to guarantee that no two
 * concurrent requests can lock or book the same seat simultaneously.</p>
 */
@SpringBootApplication
@EnableScheduling
public class SeatLockApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(SeatLockApplication.class, args);
    }

    /**
     * Automatically reads .env from current directory or parent directory
     * and sets System properties if not already set.
     */
    private static void loadDotEnv() {
        File[] candidatePaths = {
            new File(".env"),
            new File("../.env")
        };
        for (File file : candidatePaths) {
            if (file.exists() && file.isFile()) {
                try {
                    List<String> lines = Files.readAllLines(file.toPath());
                    for (String line : lines) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                            continue;
                        }
                        int eqIndex = line.indexOf('=');
                        String key = line.substring(0, eqIndex).trim();
                        String value = line.substring(eqIndex + 1).trim();
                        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                            (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, value);
                        }
                    }
                } catch (IOException ignored) {
                }
                break;
            }
        }
    }
}
