package com.languageui.api.friend;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;

public record AddFriendRequest(@NotNull UUID userId) {
}
