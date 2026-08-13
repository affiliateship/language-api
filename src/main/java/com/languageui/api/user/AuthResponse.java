package com.languageui.api.user;

public record AuthResponse(String accessToken, UserAccount user) {
}
