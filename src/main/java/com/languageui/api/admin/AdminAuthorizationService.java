package com.languageui.api.admin;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.languageui.api.user.AuthService;
import com.languageui.api.user.AuthorizationException;
import com.languageui.api.user.UserAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthorizationService {
    private final AuthService authService;
    private final Set<String> adminEmails;

    public AdminAuthorizationService(AuthService authService,
            @Value("${app.admin.emails:}") String configuredEmails) {
        this.authService = authService;
        this.adminEmails = Arrays.stream(configuredEmails.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .map(email -> email.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public UserAccount requireAdmin(String authorization) {
        UserAccount user = authService.currentUser(authorization);
        if (!adminEmails.contains(user.email().toLowerCase(Locale.ROOT))) {
            throw new AuthorizationException("Administrator access is required");
        }
        return user;
    }
}
