package com.languageui.api.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFeedbackRequest(
        @NotNull FeedbackCategory category,
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 4000) String message) {
}
