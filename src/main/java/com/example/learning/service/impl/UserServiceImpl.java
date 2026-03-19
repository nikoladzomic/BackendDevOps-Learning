package com.example.learning.service.impl;

import com.example.learning.dto.auth.CreateUserRequest;
import com.example.learning.dto.UserDTO;
import com.example.learning.entity.Role;
import com.example.learning.entity.User;
import com.example.learning.exception.ConflictException;
import com.example.learning.exception.ResourceNotFoundException;
import com.example.learning.repository.UserRepository;
import com.example.learning.repository.RoleRepository;
import com.example.learning.service.CurrentUserProvider;
import com.example.learning.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public UserDTO create(CreateUserRequest dto) {
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        // set roles
        for (String roleName : dto.getRoles()) {
            Role role = roleRepository.findByName(roleName);
            user.getRoles().add(role);
        }

        userRepository.save(user);
        return mapToDTO(user);
    }

    @Override
    public UserDTO get(Long id) {
        return mapToDTO(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id)));
    }

    @Override
    public List<UserDTO> getAll() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserDTO update(Long id, UserDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        return mapToDTO(userRepository.save(user));
    }

    @Override
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public UserDTO getCurrentUser() {
        Long userId = currentUserProvider.getCurrentUserId();
        return mapToDTO(userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    private UserDTO mapToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEnabled(user.getEnabled());

        dto.setRoles(
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
        );

        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }

    @Override
    @Transactional
    public void setBanStatus(Long id, boolean banned) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setEnabled(!banned);
        userRepository.save(user);
        log.info("User {} ban status set to: {}", id, banned);
    }

    @Override
    @Transactional
    public void promoteToAdmin(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        Role adminRole = roleRepository.findByName("ROLE_ADMIN");
        if (adminRole == null) {
            throw new ResourceNotFoundException("ROLE_ADMIN not found");
        }

        if (user.getRoles().contains(adminRole)) {
            throw new ConflictException("User is already an admin");
        }

        user.getRoles().add(adminRole);
        userRepository.save(user);
        log.info("User {} promoted to admin", id);
    }

    @Override
    @Transactional
    public void demoteFromAdmin(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        Role adminRole = roleRepository.findByName("ROLE_ADMIN");
        if (adminRole == null) {
            throw new ResourceNotFoundException("ROLE_ADMIN not found");
        }

        if (!user.getRoles().contains(adminRole)) {
            throw new ConflictException("User is not an admin");
        }

        //Sprecavamo da se obrise poslednji admin
        long adminCount = userRepository.countByRole("ROLE_ADMIN");
        if (adminCount <= 1) {
            throw new ConflictException("Cannot demote the last admin");
        }

        user.getRoles().remove(adminRole);
        userRepository.save(user);
        log.info("User {} demoted from admin", id);
    }
}
