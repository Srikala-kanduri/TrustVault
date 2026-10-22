
package com.trustvault;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@Order(3)
public class DevViewerInitializer implements CommandLineRunner {

    private final UserService userService;

    @Value("${trustvault.dev-viewer.enabled:false}")
    private boolean enabled;

    @Value("${trustvault.dev-viewer.email:viewer@trustvault.local}")
    private String email;

    @Value("${trustvault.dev-viewer.password:}")
    private String password;

    public DevViewerInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        if (password == null || password.length() < 12) {
            throw new IllegalStateException(
                "A strong development viewer password must be configured"
            );
        }

        try {
            userService.createUser(email, password, UserRole.VIEWER);
            System.out.println("Development viewer account created.");
        } catch (IllegalArgumentException ex) {
            if ("An account with this email already exists".equals(ex.getMessage())) {
                System.out.println("Development viewer account already exists.");
            } else {
                throw ex;
            }
        }
    }
}
