package com.languageui.api.word;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WordExample(
        @NotBlank @Size(max = 2000) String text,
        @NotBlank @Size(max = 2000) String englishTranslation) {
}
