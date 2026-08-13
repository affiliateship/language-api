package com.languageui.api.friend;

import java.time.LocalDateTime;
import java.util.UUID;

public record FriendRequestResponse(UUID id, FriendSummary requester, LocalDateTime createdAt) {
}
