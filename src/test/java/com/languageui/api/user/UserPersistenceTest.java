package com.languageui.api.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UserPersistenceTest {
    @Autowired private UserService service;
    @Autowired private UserRepository repository;

    @Test
    void persistsAccountCredentialsAndLanguageEnrollment() {
        String suffix = UUID.randomUUID().toString();
        UserAccount created = service.create(new CreateUserRequest(
                "persist-" + suffix + "@example.com", "Persistent", "Learner", "password123"),
                "learner." + suffix.substring(0, 8));

        service.addLanguage(created.id(), "zh");

        UserAccount loaded = service.findById(created.id());
        UserRepository.StoredUser stored = repository.findById(created.id()).orElseThrow();
        assertThat(loaded.email()).isEqualTo(created.email());
        assertThat(loaded.languageCodes()).containsExactly("zh");
        assertThat(stored.passwordHash()).isNotEqualTo("password123").startsWith("$2");
        assertThat(service.authenticate(created.email(), "password123").id()).isEqualTo(created.id());
    }
}
