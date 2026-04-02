package com.example.learning.config;

import com.example.learning.entity.Role;
import com.example.learning.entity.User;
import com.example.learning.repository.RoleRepository;
import com.example.learning.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@admin.com}")
    private String adminEmail;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Value("${app.admin.firstName:Admin}")
    private String adminFirstName;

    @Override
    @Transactional
    public void run(String... args) {

        Role adminRole = roleRepository.findByName("ROLE_ADMIN");
        Role userRole = roleRepository.findByName("ROLE_USER");

        if (adminRole == null) {
            adminRole = roleRepository.save(new Role("ROLE_ADMIN"));
        }

        if (userRole == null) {
            userRole = roleRepository.save(new Role("ROLE_USER"));
        }

        if (userRepository.findByEmail(adminEmail).isEmpty()) {

            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setFirstName(adminFirstName);
            admin.setLastName("Admin");
            admin.setCreatedAt(new Date());
            admin.setEnabled(true);
            admin.setEmailVerified(true);
            admin.getRoles().add(adminRole);
            userRepository.save(admin);
        }
    }
}


