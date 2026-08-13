package com.languageui.api.studygroup;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
public record InviteFriendsRequest(@NotEmpty @Size(max = 20) Set<UUID> friendIds) { }
