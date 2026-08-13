package com.languageui.api.topic;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.languageui.api.word.WordEntry;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/topics")
public class TopicController {

    private final TopicService service;

    public TopicController(TopicService service) { this.service = service; }

    @GetMapping
    List<Topic> findAll(@RequestParam(required = false) String language) {
        return service.findAll(language);
    }

    @GetMapping("/{topicId}")
    Topic findById(@PathVariable UUID topicId) { return service.findById(topicId); }

    @PostMapping
    ResponseEntity<Topic> create(@Valid @RequestBody CreateTopicRequest request) {
        Topic topic = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/topics/" + topic.id())).body(topic);
    }

    @GetMapping("/{topicId}/words")
    List<WordEntry> words(@PathVariable UUID topicId) { return service.words(topicId); }

    @PostMapping("/{topicId}/words/{wordId}")
    Topic addWord(@PathVariable UUID topicId, @PathVariable UUID wordId) {
        return service.addWord(topicId, wordId);
    }

    @DeleteMapping("/{topicId}/words/{wordId}")
    ResponseEntity<Void> removeWord(@PathVariable UUID topicId, @PathVariable UUID wordId) {
        service.removeWord(topicId, wordId);
        return ResponseEntity.noContent().build();
    }
}
