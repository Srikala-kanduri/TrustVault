
package com.trustvault;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    @GetMapping("/api/me")
    public String currentUser(Authentication authentication) {
        return "Authenticated as: " + authentication.getName()
                + " | Authorities: " + authentication.getAuthorities();
    }
}
