package com.languageui.api.learning;

import java.util.UUID;

public record Level(UUID id, String languageCode, String code, String name, int sequence) {
}
