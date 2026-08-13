package com.languageui.api.language;

import java.util.UUID;

public class LanguageNotFoundException extends RuntimeException {

    public LanguageNotFoundException(UUID id) {
        super("Language %s was not found".formatted(id));
    }
}
