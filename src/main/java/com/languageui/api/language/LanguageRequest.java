package com.languageui.api.language;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LanguageRequest(
        @NotBlank
        @Pattern(regexp = "^[a-zA-Z]{2,3}(-[a-zA-Z]{2,4})?$", message = "must be a valid language code")
        String code,
        @NotBlank @Size(max = 100) String name) {
}
