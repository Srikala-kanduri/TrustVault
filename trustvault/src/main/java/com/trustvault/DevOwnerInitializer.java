
package com.trustvault;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@Order(2)
public class DevOwnerInitializer implements CommandLineRunner {

    private final UserService userService;

    @Value("${trustvault.dev-owner.enabled:false}")
    private boolean enabled;

    @Value("${trustvault.dev-owner.email:owner@trustvault.local}")
    private String email;

    @Value("${trustvault.dev-owner.password:}")
    private String password;

    public DevOwnerInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        if (password == null || password.length() < 12) {
            throw new IllegalStateException(
                "A strong development owner password must be configured"
            );
        }

        try {
            userService.createUser(email, password, UserRole.OWNER);
            System.out.println("Development owner account created.");
        } catch (IllegalArgumentException ex) {
            if ("An account with this email already exists".equals(ex.getMessage())) {
                System.out.println("Development owner account already exists.");
            } else {
                throw ex;
            }
        }
    }
}
