package com.languageui.api.feedback;

import java.time.LocalDateTime;
import java.util.UUID;

public record Feedback(UUID id, UUID userId, FeedbackCategory category, String title,
                       String message, String status, LocalDateTime createdAt) {
}
