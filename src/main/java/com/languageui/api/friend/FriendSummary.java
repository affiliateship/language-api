package com.languageui.api.friend;

import java.util.List;
import java.util.UUID;
import com.languageui.api.language.Language;

public record FriendSummary(UUID id, String username, String firstName, String lastName,
                            List<Language> learningLanguages) {
}
