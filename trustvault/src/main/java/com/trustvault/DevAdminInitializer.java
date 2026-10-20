
package com.trustvault;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevAdminInitializer implements CommandLineRunner {

    private final UserService userService;

    @Value("${trustvault.dev-admin.enabled:false}")
    private boolean enabled;

    @Value("${trustvault.dev-admin.email}")
    private String email;

    @Value("${trustvault.dev-admin.password:}")
    private String password;

    public DevAdminInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        if (password == null || password.length() < 12) {
            throw new IllegalStateException(
                "A strong development admin password must be configured"
            );
        }

        try {
            userService.createUser(email, password, UserRole.ADMIN);
            System.out.println("Development admin account created.");
        } catch (IllegalArgumentException ex) {
            if ("An account with this email already exists".equals(ex.getMessage())) {
                System.out.println("Development admin account already exists.");
            } else {
                throw ex;
            }
        }
    }
}
