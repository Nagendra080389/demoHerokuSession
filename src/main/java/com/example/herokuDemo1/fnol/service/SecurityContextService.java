package com.example.herokuDemo1.fnol.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service for accessing security context information.
 */
@Service
public class SecurityContextService {

    /**
     * Gets the principal name from the security context.
     *
     * @return the principal name, or "system" if not authenticated
     */
    public String getPrincipalName() {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context == null) {
            return "system";
        }

        Authentication authentication = context.getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }

        String name = authentication.getName();
        return (name != null) ? name : "system";
    }
}
