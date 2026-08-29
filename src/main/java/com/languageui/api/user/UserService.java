package com.languageui.api.user;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.languageui.api.language.Language;
import com.languageui.api.language.LanguageService;
import com.languageui.api.user.UserRepository.StoredUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository repository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final LanguageService languageService;
    private final Duration usernameChangeCooldown;

    public UserService(UserRepository repository, LanguageService languageService,
            @Value("${app.username.change-cooldown:30d}") Duration usernameChangeCooldown) {
        this.repository = repository;
        this.languageService = languageService;
        this.usernameChangeCooldown = usernameChangeCooldown;
    }

    public List<UserAccount> findAll() {
        return repository.findAll().stream().map(this::toAccount).toList();
    }

    public UserAccount findById(UUID id) {
        return toAccount(stored(id));
    }

    public UserAccount findByUsername(String value) {
        String username = normalizeUsername(value);
        return repository.findByUsername(username).map(this::toAccount)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    public UserAccount create(CreateUserRequest request) {
        return create(request, null);
    }

    @Transactional
    public UserAccount create(CreateUserRequest request, String requestedUsername) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (repository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("An account already exists for " + email);
        }
        String username = requestedUsername == null || requestedUsername.isBlank()
                ? availableUsername(email.substring(0, email.indexOf('@')))
                : requireAvailableUsername(requestedUsername, null);
        LocalDateTime usernameChangedAt = requestedUsername == null || requestedUsername.isBlank()
                ? null : LocalDateTime.now();
        StoredUser user = new StoredUser(UUID.randomUUID(), email, username, usernameChangedAt,
                request.firstName().trim(), request.lastName().trim(),
                passwordEncoder.encode(request.password()));
        repository.insert(user);
        return toAccount(user);
    }

    public UserAccount authenticate(String emailAddress, String password) {
        String email = emailAddress.trim().toLowerCase(Locale.ROOT);
        StoredUser user = repository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));
        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw new AuthenticationException("Invalid email or password");
        }
        return toAccount(user);
    }

    @Transactional
    public UserAccount addLanguage(UUID userId, String languageCode) {
        stored(userId);
        String code = languageService.findByCode(languageCode).code();
        repository.addLanguage(userId, code);
        return findById(userId);
    }

    public List<Language> learningLanguages(UUID userId) {
        stored(userId);
        return repository.languageCodes(userId).stream()
                .map(languageService::findByCode)
                .sorted(java.util.Comparator.comparing(Language::name))
                .toList();
    }

    @Transactional
    public UserAccount update(UUID userId, UpdateProfileRequest request) {
        StoredUser current = stored(userId);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (repository.findByEmail(email).filter(user -> !user.id().equals(userId)).isPresent()) {
            throw new IllegalStateException("An account already exists for " + email);
        }
        String username = current.username();
        LocalDateTime usernameChangedAt = current.usernameChangedAt();
        if (request.username() != null && !normalizeUsername(request.username()).equals(username)) {
            LocalDateTime availableAt = usernameChangedAt == null ? null
                    : usernameChangedAt.plus(usernameChangeCooldown);
            if (availableAt != null && LocalDateTime.now().isBefore(availableAt)) {
                throw new IllegalStateException("Username can be changed again after " + availableAt);
            }
            username = requireAvailableUsername(request.username(), userId);
            usernameChangedAt = LocalDateTime.now();
        }
        StoredUser updated = new StoredUser(current.id(), email, username, usernameChangedAt,
                request.firstName().trim(), request.lastName().trim(), current.passwordHash());
        repository.update(updated);
        return toAccount(updated);
    }

    @Transactional
    public void delete(UUID id) {
        if (repository.delete(id) == 0) {
            throw new IllegalArgumentException("User not found: " + id);
        }
    }

    private StoredUser stored(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    private UserAccount toAccount(StoredUser user) {
        Set<String> languages = repository.languageCodes(user.id());
        return new UserAccount(user.id(), user.email(), user.username(), user.firstName(),
                user.lastName(), Set.copyOf(languages));
    }

    private String requireAvailableUsername(String value, UUID currentUserId) {
        String username = normalizeUsername(value);
        if (repository.findByUsername(username)
                .filter(user -> !user.id().equals(currentUserId)).isPresent()) {
            throw new IllegalStateException("Username is already taken: " + username);
        }
        return username;
    }

    private String availableUsername(String value) {
        String base = normalizeUsernameCandidate(value);
        String candidate = base;
        int suffix = 2;
        while (repository.findByUsername(candidate).isPresent()) candidate = base + suffix++;
        return candidate;
    }

    private String normalizeUsernameCandidate(String value) {
        String base = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._]", "");
        if (base.length() < 3) base = "user";
        return base.substring(0, Math.min(base.length(), 24));
    }

    private String normalizeUsername(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Username is required");
        String username = value.trim().toLowerCase(Locale.ROOT);
        if (username.length() < 3 || username.length() > 30
                || !username.matches("[a-z0-9._]+")) {
            throw new IllegalArgumentException(
                    "Username must be 3-30 characters using letters, numbers, periods, or underscores");
        }
        return username;
    }
}
