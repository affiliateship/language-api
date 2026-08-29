package com.languageui.api.dailyword;

import com.languageui.api.user.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

@RestController
@RequestMapping("/api/me/daily-words")
public class DailyWordController {
    private final DailyWordService service;
    private final AuthService authService;

    public DailyWordController(DailyWordService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @GetMapping
    DailyWordsResponse dailyWords(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return service.dailyWords(authService.currentUserId(authorization));
    }

    @GetMapping("/preferences")
    DailyWordPreferences preferences(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return service.preferences(authService.currentUserId(authorization));
    }

    @PutMapping("/preferences")
    DailyWordPreferences update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody UpdateDailyWordPreferencesRequest request) {
        return service.update(authService.currentUserId(authorization), request);
    }

    @PostMapping("/{wordId}/view")
    DailyWordProgress markViewed(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID wordId) {
        return service.markViewed(authService.currentUserId(authorization), wordId);
    }

    @PostMapping("/{wordId}/answer")
    DailyWordProgress answer(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID wordId,
            @RequestBody AnswerDailyWordRequest request) {
        return service.answer(authService.currentUserId(authorization), wordId, request.correct());
    }

    @PostMapping("/{wordId}/complete")
    DailyWordsResponse complete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID wordId) {
        return service.complete(authService.currentUserId(authorization), wordId);
    }
}
