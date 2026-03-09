package com.example.learning.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {


    @GetMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminOnly() {

        log.debug("Admin endpoint /api//auth/login called");
        return "Admin access granted successfully";
    }

}
 