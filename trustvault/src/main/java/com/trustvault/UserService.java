
package com.trustvault;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
public class UserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser createUser(
            String email,
            String rawPassword,
            UserRole role) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (rawPassword == null || rawPassword.length() < 12) {
            throw new IllegalArgumentException(
                "Password must contain at least 12 characters"
            );
        }

        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }

        String normalizedEmail = email.trim().toLowerCase();

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException(
                "An account with this email already exists"
            );
        }

        String hashedPassword = passwordEncoder.encode(rawPassword);

        AppUser user = new AppUser(
            normalizedEmail,
            hashedPassword,
            role
        );

        return userRepository.save(user);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean createInitialUsers(
            String adminEmail, String adminPassword,
            String ownerEmail, String ownerPassword,
            String viewerEmail, String viewerPassword) {
        if (userRepository.count() != 0) {
            return false;
        }
        createUser(adminEmail, adminPassword, UserRole.ADMIN);
        createUser(ownerEmail, ownerPassword, UserRole.OWNER);
        createUser(viewerEmail, viewerPassword, UserRole.VIEWER);
        return true;
    }
}
