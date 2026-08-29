package com.languageui.api.friend;

import java.util.List;
import java.util.UUID;

import com.languageui.api.user.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/me")
public class FriendController {
    private final FriendService service;
    private final AuthService authService;

    public FriendController(FriendService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @GetMapping("/friends")
    List<FriendSummary> friends(@RequestHeader(value = "Authorization", required = false) String auth) {
        return service.friends(authService.currentUserId(auth));
    }

    @PostMapping("/friend-requests")
    FriendRequestResponse request(@RequestHeader(value = "Authorization", required = false) String auth,
            @Valid @RequestBody AddFriendRequest request) {
        return service.request(authService.currentUserId(auth), request.username());
    }

    @GetMapping("/friend-search")
    List<FriendSearchResult> search(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam String username) {
        return service.search(authService.currentUserId(auth), username);
    }

    @GetMapping("/friend-requests")
    List<FriendRequestResponse> requests(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        return service.incoming(authService.currentUserId(auth));
    }

    @PostMapping("/friend-requests/{requestId}/accept")
    List<FriendSummary> accept(@RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable UUID requestId) {
        return service.accept(authService.currentUserId(auth), requestId);
    }

    @DeleteMapping("/friend-requests/{requestId}")
    ResponseEntity<Void> decline(@RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable UUID requestId) {
        service.declineOrCancel(authService.currentUserId(auth), requestId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/friends/{friendId}")
    ResponseEntity<Void> remove(@RequestHeader(value = "Authorization", required = false) String auth,
            @PathVariable UUID friendId) {
        service.remove(authService.currentUserId(auth), friendId);
        return ResponseEntity.noContent().build();
    }
}
