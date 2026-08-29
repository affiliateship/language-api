package com.languageui.api.user;

import java.util.Set;
import java.util.UUID;

public record UserAccount(UUID id, String email, String username, String firstName, String lastName,
                          Set<String> languageCodes) {
}
