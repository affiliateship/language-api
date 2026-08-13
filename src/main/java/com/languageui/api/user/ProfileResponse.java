package com.languageui.api.user;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.languageui.api.language.Language;

public record ProfileResponse(UUID id, String email, String firstName, String lastName,
                              String displayName, Set<String> languages,
                              List<Language> learningLanguages) {

    static ProfileResponse from(UserAccount user, List<Language> learningLanguages) {
        return new ProfileResponse(user.id(), user.email(), user.firstName(), user.lastName(),
                user.displayName(), user.languageCodes(), learningLanguages);
    }
}
