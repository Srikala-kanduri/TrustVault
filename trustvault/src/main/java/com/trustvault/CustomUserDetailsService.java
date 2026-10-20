
package com.trustvault;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;

    public CustomUserDetailsService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        AppUser appUser = userRepository.findByEmail(
                email.trim().toLowerCase()
            )
            .orElseThrow(() ->
                new UsernameNotFoundException("Invalid email or password")
            );

        return User.withUsername(appUser.getEmail())
            .password(appUser.getPassword())
            .roles(appUser.getRole().name())
            .build();
    }
}
