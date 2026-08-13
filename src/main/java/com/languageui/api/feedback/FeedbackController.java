package com.languageui.api.feedback;

import java.net.URI;
import java.util.List;

import com.languageui.api.user.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/feedback")
public class FeedbackController {
    private final FeedbackRepository repository;
    private final AuthService authService;

    public FeedbackController(FeedbackRepository repository, AuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    @PostMapping
    ResponseEntity<Feedback> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateFeedbackRequest request) {
        Feedback feedback = repository.save(authService.currentUserId(authorization), request);
        return ResponseEntity.created(URI.create("/api/me/feedback/" + feedback.id())).body(feedback);
    }

    @GetMapping
    List<Feedback> mine(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return repository.findByUser(authService.currentUserId(authorization));
    }
}
