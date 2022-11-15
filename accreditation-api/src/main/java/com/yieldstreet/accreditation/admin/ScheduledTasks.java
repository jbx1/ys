package com.yieldstreet.accreditation.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

    private final AdminService adminService;

    public ScheduledTasks(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Run this every day at midnight.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void expireOldConfirmed() {
        logger.info("Running scheduled task to expire old confirmed accreditation requests.");
        adminService.expireOldConfirmedAccreditations();
    }
}

