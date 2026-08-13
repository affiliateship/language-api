package com.languageui.api.topic;

import java.util.UUID;

public record Topic(UUID id, String language, String name, String description, long wordCount) {
}
