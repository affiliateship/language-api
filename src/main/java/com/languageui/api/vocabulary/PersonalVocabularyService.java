package com.languageui.api.vocabulary;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.languageui.api.user.UserService;
import com.languageui.api.word.WordCatalogService;
import com.languageui.api.word.WordEntry;
import org.springframework.stereotype.Service;

@Service
public class PersonalVocabularyService {

    private final Map<UUID, Set<UUID>> lists = new ConcurrentHashMap<>();
    private final UserService userService;
    private final WordCatalogService wordCatalogService;

    public PersonalVocabularyService(UserService userService, WordCatalogService wordCatalogService) {
        this.userService = userService;
        this.wordCatalogService = wordCatalogService;
    }

    public List<WordEntry> findAll(UUID userId) {
        userService.findById(userId);
        return lists.getOrDefault(userId, Set.of()).stream().map(wordCatalogService::findById).toList();
    }

    public List<UUID> vocabularyIds(UUID userId) {
        userService.findById(userId);
        return List.copyOf(lists.getOrDefault(userId, Set.of()));
    }

    public List<WordEntry> add(UUID userId, UUID vocabularyId) {
        userService.findById(userId);
        wordCatalogService.findById(vocabularyId);
        lists.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(vocabularyId);
        return findAll(userId);
    }

    public void remove(UUID userId, UUID vocabularyId) {
        userService.findById(userId);
        Set<UUID> vocabularyIds = lists.get(userId);
        if (vocabularyIds != null) {
            vocabularyIds.remove(vocabularyId);
        }
    }
}
