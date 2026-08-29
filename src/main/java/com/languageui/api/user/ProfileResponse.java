package com.languageui.api.user;

import java.util.List;
import java.util.UUID;
import com.languageui.api.language.Language;

public record ProfileResponse(UUID id, String email, String username, String firstName, String lastName,
                              List<Language> learningLanguages) {

    static ProfileResponse from(UserAccount user, List<Language> learningLanguages) {
        return new ProfileResponse(user.id(), user.email(), user.username(), user.firstName(), user.lastName(),
                learningLanguages);
    }
}
