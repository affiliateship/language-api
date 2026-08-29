package com.languageui.api.friend;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddFriendRequest(
        @NotBlank @Size(min = 3, max = 30)
        @Pattern(regexp = "[A-Za-z0-9._]+", message = "must be a username")
        String username) {
}
