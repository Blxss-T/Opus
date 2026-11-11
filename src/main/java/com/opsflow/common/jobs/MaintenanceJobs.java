package com.opsflow.common.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "opus.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class MaintenanceJobs {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceJobs.class);

    private final JdbcTemplate jdbcTemplate;

    public MaintenanceJobs(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedDelayString = "${opus.jobs.reporting-refresh-ms:300000}")
    public void refreshReportingViews() {
        try {
            jdbcTemplate.execute("SELECT reporting.refresh_sales_mvs()");
        } catch (RuntimeException ex) {
            log.warn("Reporting materialized view refresh skipped: {}", ex.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${opus.jobs.otp-cleanup-ms:3600000}")
    public void cleanupExpiredSessions() {
        try {
            jdbcTemplate.execute("SELECT identity.cleanup_expired_sessions()");
        } catch (RuntimeException ex) {
            log.warn("Session cleanup skipped: {}", ex.getMessage());
        }
    }
}
