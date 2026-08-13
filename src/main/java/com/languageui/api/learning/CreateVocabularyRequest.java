package com.languageui.api.learning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVocabularyRequest(
        @NotBlank @Size(max = 200) String term,
        @NotBlank @Size(max = 300) String translation,
        @Size(max = 200) String pronunciation) {
}
