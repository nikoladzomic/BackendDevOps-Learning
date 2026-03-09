package com.example.learning.dto;

import java.util.Date;
import java.util.Set;
import lombok.*;


@Data
public class UserDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Set<String> roles;
    private Boolean enabled;
    private Date createdAt;
    private Date updatedAt;

}
