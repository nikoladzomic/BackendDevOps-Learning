package com.example.learning.config;


import com.example.learning.entity.Role;
import com.example.learning.entity.User;
import com.example.learning.repository.RoleRepository;
import com.example.learning.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        if (userRepository.findByEmail("admin@admin.com").isPresent()) {
            return;
        }

            Role adminRole = roleRepository.findByName("ROLE_ADMIN");

            if (adminRole == null) {
                throw new RuntimeException("ADMIN role not found");
            }

            User admin = new User();
            admin.setEmail("admin@admin.com");
            admin.setFirstName("Super");
            admin.setLastName("Admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEnabled(true);
            admin.setCreatedAt(new Date());

            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);
            admin.setRoles(roles);

            userRepository.save(admin);

            System.out.println(">>>>>Admin user created<<<<<");


    }
}