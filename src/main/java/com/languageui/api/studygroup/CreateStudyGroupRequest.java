package com.languageui.api.studygroup;

import java.util.Set;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStudyGroupRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank String language,
        @Size(max = 10) String level,
        @Size(max = 20) Set<UUID> friendIds) { }
