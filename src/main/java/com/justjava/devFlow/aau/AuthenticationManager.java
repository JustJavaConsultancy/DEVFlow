package com.justjava.devFlow.aau;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class AuthenticationManager {

    public Object get(String fieldName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof DefaultOidcUser)) {
            return null;
        }
        DefaultOidcUser defaultOidcUser = (DefaultOidcUser) authentication.getPrincipal();
        return defaultOidcUser.getClaims().get(fieldName);
    }

    @SuppressWarnings("unchecked")
    private List<String> getGroups() {
        Object groupsObj = get("groups");
        if (groupsObj instanceof List<?>) {
            return (List<String>) groupsObj;
        }
        if (groupsObj instanceof String) {
            return List.of((String) groupsObj);
        }
        return Collections.emptyList();
    }

    public Boolean isMerchant() {
        return getGroups().stream().anyMatch(group -> "/merchant".equalsIgnoreCase(group));
    }

    public Boolean isComplianceOfficer() {
        return getGroups().stream().anyMatch(group -> "/compliance".equalsIgnoreCase(group));
    }

    public Boolean isCustomerSupport() {
        return getGroups().stream().anyMatch(group -> "/customer-support".equalsIgnoreCase(group));
    }

    public Boolean isPgAdmin() {
        return getGroups().stream().anyMatch(group -> "/pgAdmin".equalsIgnoreCase(group));
    }

    @SuppressWarnings("unchecked")
    private List<String> getAllGroups() {
        Object groupsObj = get("groups"); // ✅ now looking for "groups"
        if (groupsObj instanceof List<?>) {
            return (List<String>) groupsObj;
        }
        if (groupsObj instanceof String) {
            return List.of((String) groupsObj);
        }
        return Collections.emptyList();
    }

    public boolean isAdmin() {
        return getAllGroups().stream()
                .anyMatch(group -> "/admin".equalsIgnoreCase(group));
    }

}
