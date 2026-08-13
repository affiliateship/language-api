package com.languageui.api.user;

import java.util.Set;
import java.util.UUID;

public record UserAccount(UUID id, String email, String firstName, String lastName,
                          String displayName, Set<String> languageCodes) {
}
