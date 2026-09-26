package com.backendprinciple.playground.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates the first admin from APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD, if set and not already present. */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository users, PasswordEncoder passwordEncoder,
                          @Value("${app.admin.email:}") String email,
                          @Value("${app.admin.password:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        if (password.length() < 12) {
            log.warn("APP_ADMIN_PASSWORD is shorter than 12 characters - admin not created");
            return;
        }
        String normalized = User.normalizeEmail(email);
        users.findByEmail(normalized).ifPresentOrElse(u -> {
            if (u.getRole() != Role.ADMIN) {
                u.setRole(Role.ADMIN);
                log.info("Promoted {} to ADMIN", normalized);
            }
        }, () -> {
            users.save(new User(normalized, passwordEncoder.encode(password), "Admin", Role.ADMIN));
            log.info("Created admin account {}", normalized);
        });
    }
}
