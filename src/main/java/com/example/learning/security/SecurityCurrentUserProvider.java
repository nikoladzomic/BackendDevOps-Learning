package com.example.learning.security;

import com.example.learning.service.CurrentUserProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityCurrentUserProvider implements CurrentUserProvider{
    @Override
    public Long getCurrentUserId() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        CustomUserDetails principal =
                (CustomUserDetails) authentication.getPrincipal();

        return principal.getId();
    }
}
