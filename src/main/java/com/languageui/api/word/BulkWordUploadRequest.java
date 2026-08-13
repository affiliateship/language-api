package com.languageui.api.word;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record BulkWordUploadRequest(
        @NotEmpty @Size(max = 3000) List<@Valid CreateWordRequest> words) {
}
