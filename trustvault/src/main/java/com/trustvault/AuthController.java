
package com.trustvault;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    @GetMapping("/api/me")
    public CurrentUserResponse currentUser(Authentication authentication) {
        return new CurrentUserResponse(
                authentication.getName(),
                authentication.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .toList());
    }

    public record CurrentUserResponse(
            String email,
            List<String> authorities) {
    }
}
