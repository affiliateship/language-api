package com.languageui.api.user;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.languageui.api.vocabulary.PersonalVocabularyService;
import com.languageui.api.streak.StreakService;
import com.languageui.api.word.WordCatalogService;
import com.languageui.api.word.WordEntry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private static final String LEARNED = "learned";

    private final AuthService authService;
    private final PersonalVocabularyService personalVocabularyService;
    private final WordCatalogService wordCatalogService;
    private final UserService userService;
    private final StreakService streakService;

    public MeController(AuthService authService, PersonalVocabularyService personalVocabularyService,
            WordCatalogService wordCatalogService, UserService userService, StreakService streakService) {
        this.authService = authService;
        this.personalVocabularyService = personalVocabularyService;
        this.wordCatalogService = wordCatalogService;
        this.userService = userService;
        this.streakService = streakService;
    }

    @GetMapping("/profile")
    ProfileResponse profile(@RequestHeader(value = "Authorization", required = false) String authorization) {
        UserAccount user = authService.currentUser(authorization);
        return ProfileResponse.from(user, userService.learningLanguages(user.id()));
    }

    @PatchMapping("/profile")
    ProfileResponse updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = authService.currentUserId(authorization);
        UserAccount user = userService.update(userId, request);
        return ProfileResponse.from(user, userService.learningLanguages(userId));
    }

    @GetMapping("/streak")
    StreakResponse streak(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return streakService.get(authService.currentUserId(authorization));
    }

    @PostMapping("/streak/check-in")
    StreakResponse checkIn(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return streakService.record(authService.currentUserId(authorization));
    }

    @GetMapping("/vocabulary")
    List<PersonalVocabularyResponse> vocabulary(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String status) {
        UUID userId = authService.currentUserId(authorization);
        String languageCode = language == null ? null : languageCode(language);
        if (status != null && !LEARNED.equalsIgnoreCase(status)) {
            return List.of();
        }
        return personalVocabularyService.vocabularyIds(userId).stream()
                .filter(id -> languageCode == null
                        || languageCode(wordCatalogService.findById(id)).equals(languageCode))
                .map(this::response)
                .toList();
    }

    private PersonalVocabularyResponse response(UUID vocabularyId) {
        WordEntry item = wordCatalogService.findById(vocabularyId);
        return new PersonalVocabularyResponse(item.id(), item.language(), LEARNED,
                item.word(), item.englishTranslation(), item.pronunciation(), item.pinyin(), item.level(),
                item.wordTypes());
    }

    private String languageCode(String language) {
        return switch (language.trim().toLowerCase(Locale.ROOT)) {
            case "chinese", "zh" -> "zh";
            case "spanish", "es" -> "es";
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }

    private String languageCode(WordEntry word) {
        return word.language().equals("Chinese") ? "zh" : "es";
    }
}
