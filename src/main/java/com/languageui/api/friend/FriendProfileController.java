package com.languageui.api.friend;

import java.util.UUID;

import com.languageui.api.user.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class FriendProfileController {
    private final FriendProfileService service;
    private final AuthService authService;

    public FriendProfileController(FriendProfileService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @GetMapping("/friends/{friendId}/profile")
    FriendProfile profile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID friendId) {
        return service.profile(authService.currentUserId(authorization), friendId);
    }

    @GetMapping("/privacy")
    FriendPrivacySettings privacy(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return service.privacy(authService.currentUserId(authorization));
    }

    @PutMapping("/privacy")
    FriendPrivacySettings updatePrivacy(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody UpdateFriendPrivacyRequest request) {
        return service.updatePrivacy(authService.currentUserId(authorization), request);
    }
}
