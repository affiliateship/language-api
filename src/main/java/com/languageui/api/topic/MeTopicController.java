package com.languageui.api.topic;

import java.util.List;
import java.util.UUID;

import com.languageui.api.user.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/topics")
public class MeTopicController {

    private final TopicService topicService;
    private final AuthService authService;

    public MeTopicController(TopicService topicService, AuthService authService) {
        this.topicService = topicService;
        this.authService = authService;
    }

    @GetMapping
    List<Topic> selected(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return topicService.selected(authService.currentUserId(authorization));
    }

    @PostMapping("/{topicId}")
    List<Topic> select(@RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID topicId) {
        return topicService.select(authService.currentUserId(authorization), topicId);
    }

    @DeleteMapping("/{topicId}")
    ResponseEntity<Void> deselect(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID topicId) {
        topicService.deselect(authService.currentUserId(authorization), topicId);
        return ResponseEntity.noContent().build();
    }
}
