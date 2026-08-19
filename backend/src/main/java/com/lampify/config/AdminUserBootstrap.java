package com.lampify.config;

import com.lampify.entity.User;
import com.lampify.entity.UserRole;
import com.lampify.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class AdminUserBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserBootstrap.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${app.admin.seed-enabled:false}")
    private boolean seedEnabled;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    public AdminUserBootstrap(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            return;
        }

        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.warn("Admin seeding enabled but APP_ADMIN_EMAIL or APP_ADMIN_PASSWORD is missing");
            return;
        }

        String normalizedEmail = adminEmail.trim().toLowerCase();
        if (!isValidEmail(normalizedEmail)) {
            log.error(
                    "Admin seeding skipped: APP_ADMIN_EMAIL must be a full email address (example: admin@estvalgus.local). Got: {}",
                    normalizedEmail);
            return;
        }

        Optional<User> existingOptional = userRepository.findByEmail(normalizedEmail);
        if (existingOptional.isPresent()) {
            User existing = existingOptional.get();
            existing.setRole(UserRole.ADMIN);
            existing.setEnabled(true);
            existing.setPasswordLoginEnabled(true);
            existing.setPassword(passwordEncoder.encode(adminPassword));
            userRepository.save(existing);
            log.info("Updated seeded admin credentials for {}", normalizedEmail);
            return;
        }

        User admin = new User();
        admin.setEmail(normalizedEmail);
        admin.setUsername(normalizedEmail.split("@", 2)[0]);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(UserRole.ADMIN);
        admin.setEnabled(true);
        admin.setPasswordLoginEnabled(true);
        admin.setTwoFactorEnabled(false);
        userRepository.save(admin);

        log.info("Seeded default admin user {}", normalizedEmail);
    }

    private static boolean isValidEmail(String email) {
        return email != null
                && email.length() >= 8
                && email.contains("@")
                && email.contains(".");
    }
}
