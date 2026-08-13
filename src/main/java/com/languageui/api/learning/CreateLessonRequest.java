package com.languageui.api.learning;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLessonRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 1000) String description,
        @Min(1) int sequence) {
}
