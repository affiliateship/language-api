package com.languageui.api.admin;

import java.util.List;

import com.languageui.api.reading.CreateReadingLessonRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record BulkReadingLessonUploadRequest(
        @NotEmpty @Size(max = 100) List<@Valid CreateReadingLessonRequest> lessons) {
}
