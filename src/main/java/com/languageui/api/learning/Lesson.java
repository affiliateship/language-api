package com.languageui.api.learning;

import java.util.UUID;

public record Lesson(UUID id, UUID levelId, String title, String description, int sequence) {
}
