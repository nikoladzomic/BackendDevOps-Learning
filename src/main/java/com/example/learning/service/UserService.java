package com.example.learning.service;


import com.example.learning.dto.UserDTO;

import java.util.List;

public interface  UserService {

    UserDTO create(UserDTO userDTO);
    UserDTO get(Long id);
    List<UserDTO> getAll();
    UserDTO update(Long id, UserDTO userDTO);
    void delete(Long id);
    UserDTO getCurrentUser();
}
