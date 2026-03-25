package com.example.learning.dto;

import lombok.Data;

@Data
public class UserFilterRequest {
    private String email;
    private String firstName;
    private Boolean enabled;
    private int page = 0;
    private int size = 10;
    private String sortBy = "createdAt";
    private String sortDirection = "desc";
}