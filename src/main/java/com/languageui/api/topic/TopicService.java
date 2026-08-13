package com.languageui.api.topic;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.languageui.api.user.UserService;
import com.languageui.api.word.WordCatalogService;
import com.languageui.api.word.WordEntry;
import org.springframework.stereotype.Service;

@Service
public class TopicService {

    private final TopicRepository repository;
    private final WordCatalogService wordCatalogService;
    private final UserService userService;

    public TopicService(TopicRepository repository, WordCatalogService wordCatalogService,
            UserService userService) {
        this.repository = repository;
        this.wordCatalogService = wordCatalogService;
        this.userService = userService;
    }

    public Topic create(CreateTopicRequest request) {
        String language = normalizeLanguage(request.language());
        String name = request.name().trim();
        if (repository.existsByLanguageAndName(language, name)) {
            throw new IllegalStateException("Topic already exists: " + name);
        }
        Topic topic = new Topic(UUID.randomUUID(), language, name,
                request.description() == null ? "" : request.description().trim(), 0);
        repository.save(topic);
        return topic;
    }

    public List<Topic> findAll(String language) {
        return repository.findAll(language == null ? null : normalizeLanguage(language));
    }

    public Topic findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + id));
    }

    public List<WordEntry> words(UUID topicId) {
        findById(topicId);
        return repository.wordIds(topicId).stream().map(wordCatalogService::findById).toList();
    }

    public Topic addWord(UUID topicId, UUID wordId) {
        Topic topic = findById(topicId);
        WordEntry word = wordCatalogService.findById(wordId);
        if (!topic.language().equals(word.language())) {
            throw new IllegalArgumentException("Word and topic languages must match");
        }
        repository.addWord(topicId, wordId);
        return findById(topicId);
    }

    public void removeWord(UUID topicId, UUID wordId) {
        findById(topicId);
        repository.removeWord(topicId, wordId);
    }

    public List<Topic> selected(UUID userId) {
        userService.findById(userId);
        return repository.selectedByUser(userId);
    }

    public List<Topic> select(UUID userId, UUID topicId) {
        userService.findById(userId);
        findById(topicId);
        repository.select(userId, topicId);
        return repository.selectedByUser(userId);
    }

    public void deselect(UUID userId, UUID topicId) {
        userService.findById(userId);
        repository.deselect(userId, topicId);
    }

    private String normalizeLanguage(String language) {
        return switch (language.trim().toLowerCase(Locale.ROOT)) {
            case "chinese", "zh" -> "Chinese";
            case "spanish", "es" -> "Spanish";
            default -> throw new IllegalArgumentException("Only Chinese and Spanish topics are supported");
        };
    }
}
