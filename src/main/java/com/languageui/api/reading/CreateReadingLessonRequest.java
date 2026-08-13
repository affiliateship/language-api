package com.languageui.api.reading;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReadingLessonRequest(
        @NotBlank String language,
        @NotBlank @Size(max = 10) String level,
        @NotNull LessonType lessonType,
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 8000) String originalText,
        @NotBlank @Size(max = 8000) String englishTranslation,
        @NotEmpty @Size(max = 100) List<@Valid CreateReadingWordRequest> keyWords) {
}
