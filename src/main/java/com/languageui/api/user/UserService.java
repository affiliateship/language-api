package com.languageui.api.user;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.languageui.api.language.LanguageService;
import com.languageui.api.language.Language;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final Map<UUID, StoredUser> users = new ConcurrentHashMap<>();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final LanguageService languageService;

    public UserService(LanguageService languageService) {
        this.languageService = languageService;
    }

    public List<UserAccount> findAll() {
        return users.values().stream().map(this::toAccount).toList();
    }

    public UserAccount findById(UUID id) {
        return toAccount(stored(id));
    }

    public UserAccount create(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.values().stream().anyMatch(user -> user.email().equals(email))) {
            throw new IllegalStateException("An account already exists for " + email);
        }
        StoredUser user = new StoredUser(UUID.randomUUID(), email, request.firstName().trim(),
                request.lastName().trim(), passwordEncoder.encode(request.password()),
                ConcurrentHashMap.newKeySet());
        users.put(user.id(), user);
        return toAccount(user);
    }

    public UserAccount authenticate(String emailAddress, String password) {
        String email = emailAddress.trim().toLowerCase(Locale.ROOT);
        StoredUser user = users.values().stream()
                .filter(candidate -> candidate.email().equals(email))
                .findFirst()
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));
        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw new AuthenticationException("Invalid email or password");
        }
        return toAccount(user);
    }

    public UserAccount addLanguage(UUID userId, String languageCode) {
        StoredUser user = stored(userId);
        String code = languageService.findByCode(languageCode).code();
        user.languageCodes().add(code);
        return toAccount(user);
    }

    public List<Language> learningLanguages(UUID userId) {
        return stored(userId).languageCodes().stream()
                .map(languageService::findByCode)
                .sorted(java.util.Comparator.comparing(Language::name))
                .toList();
    }

    public UserAccount update(UUID userId, UpdateProfileRequest request) {
        StoredUser current = stored(userId);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.values().stream()
                .anyMatch(user -> !user.id().equals(userId) && user.email().equals(email))) {
            throw new IllegalStateException("An account already exists for " + email);
        }
        Set<String> languageCodes = ConcurrentHashMap.newKeySet();
        if (request.learningLanguageCodes() == null) {
            languageCodes.addAll(current.languageCodes());
        } else {
            request.learningLanguageCodes().stream()
                    .map(languageService::findByCode)
                    .map(Language::code)
                    .forEach(languageCodes::add);
        }
        StoredUser updated = new StoredUser(current.id(), email, request.firstName().trim(),
                request.lastName().trim(), current.passwordHash(), languageCodes);
        users.put(userId, updated);
        return toAccount(updated);
    }

    public void delete(UUID id) {
        if (users.remove(id) == null) {
            throw new IllegalArgumentException("User not found: " + id);
        }
    }

    private StoredUser stored(UUID id) {
        StoredUser user = users.get(id);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + id);
        }
        return user;
    }

    private UserAccount toAccount(StoredUser user) {
        return new UserAccount(user.id(), user.email(), user.firstName(), user.lastName(),
                user.firstName() + " " + user.lastName(),
                Set.copyOf(new LinkedHashSet<>(user.languageCodes())));
    }

    private record StoredUser(UUID id, String email, String firstName, String lastName,
                              String passwordHash, Set<String> languageCodes) {
    }
}
