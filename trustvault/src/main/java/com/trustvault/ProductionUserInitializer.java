package com.trustvault;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionUserInitializer implements CommandLineRunner {

    private final UserService userService;

    @Value("${trustvault.bootstrap-users.enabled:false}")
    private boolean enabled;

    @Value("${trustvault.bootstrap-users.admin-email:}")
    private String adminEmail;

    @Value("${trustvault.bootstrap-users.admin-password:}")
    private String adminPassword;

    @Value("${trustvault.bootstrap-users.owner-email:}")
    private String ownerEmail;

    @Value("${trustvault.bootstrap-users.owner-password:}")
    private String ownerPassword;

    @Value("${trustvault.bootstrap-users.viewer-email:}")
    private String viewerEmail;

    @Value("${trustvault.bootstrap-users.viewer-password:}")
    private String viewerPassword;

    public ProductionUserInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        boolean created = userService.createInitialUsers(
            adminEmail, adminPassword,
            ownerEmail, ownerPassword,
            viewerEmail, viewerPassword
        );
        if (created) {
            System.out.println("Initial production accounts created.");
        } else {
            System.out.println("Production accounts already exist; bootstrap skipped.");
        }
    }
}
