package com.seatlock.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's {@code @Scheduled} infrastructure so that
 * {@link com.seatlock.service.LockExpiryService} can run periodic cleanup.
 *
 * <p>{@code @EnableScheduling} is also placed on the main application class for
 * safety, but having an explicit config class makes the intent clear and
 * provides a single place to add a custom {@code TaskScheduler} bean later.</p>
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // Uses Spring Boot's default single-thread TaskScheduler.
    // Replace with a ThreadPoolTaskScheduler bean here if you need
    // multiple scheduled tasks running in parallel.
}
