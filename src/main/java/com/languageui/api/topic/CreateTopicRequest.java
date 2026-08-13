package com.languageui.api.topic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTopicRequest(
        @NotBlank String language,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description) {
}
