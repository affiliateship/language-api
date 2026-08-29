package com.languageui.api.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record UpdateProfileRequest(
        @NotBlank @Email String email,
        @Size(min = 3, max = 30)
        @Pattern(regexp = "[A-Za-z0-9._]+", message = "must contain only letters, numbers, periods, or underscores")
        String username,
        @NotBlank @Size(max = 50) String firstName,
        @NotBlank @Size(max = 50) String lastName) {
}
