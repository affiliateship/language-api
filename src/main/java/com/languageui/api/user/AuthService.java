package com.languageui.api.user;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthService {

    private final Map<String, UUID> sessions = new ConcurrentHashMap<>();
    private final UserService userService;

    public AuthService(UserService userService) {
        this.userService = userService;
    }

    public AuthResponse signUp(SignUpRequest request) {
        UserAccount user = userService.create(
                new CreateUserRequest(request.email(), request.firstName(), request.lastName(),
                        request.password()), request.username());
        log.info("user_signed_up userId={}", user.id());
        return createSession(user);
    }

    public AuthResponse signIn(SignInRequest request) {
        UserAccount user = userService.authenticate(request.email(), request.password());
        log.info("user_signed_in userId={}", user.id());
        return createSession(user);
    }

    public UserAccount currentUser(String authorization) {
        return userService.findById(currentUserId(authorization));
    }

    public UUID currentUserId(String authorization) {
        UUID userId = sessions.get(bearerToken(authorization));
        if (userId == null) {
            throw new AuthenticationException("Invalid or expired session");
        }
        return userId;
    }

    public void signOut(String authorization) {
        UUID userId = sessions.remove(bearerToken(authorization));
        if (userId != null) {
            log.info("user_signed_out userId={}", userId);
        }
    }

    private AuthResponse createSession(UserAccount user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, user.id());
        return new AuthResponse(token, user);
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AuthenticationException("Bearer token is required");
        }
        return authorization.substring(7).trim();
    }
}
