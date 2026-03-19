package com.example.learning.service;


import com.example.learning.dto.auth.CreateUserRequest;
import com.example.learning.dto.UserDTO;

import java.util.List;

public interface  UserService {

    UserDTO create(CreateUserRequest request);

    UserDTO get(Long id);
    List<UserDTO> getAll();
    UserDTO update(Long id, UserDTO userDTO);
    void delete(Long id);
    UserDTO getCurrentUser();
    void setBanStatus(Long id, boolean banned);
    void promoteToAdmin(Long id);
    void demoteFromAdmin(Long id);
}
