package com.languageui.api.reading;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReadingWordRequest(
        @NotBlank @Size(max = 200) String original,
        @NotBlank @Size(max = 1000) String englishTranslation,
        @NotBlank @Size(max = 200) String pronunciation) {
}
