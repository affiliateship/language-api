package com.languageui.api.vocabulary;

import java.util.List;
import java.util.UUID;

import com.languageui.api.word.WordEntry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}/vocabulary")
public class PersonalVocabularyController {

    private final PersonalVocabularyService service;

    public PersonalVocabularyController(PersonalVocabularyService service) { this.service = service; }

    @GetMapping
    List<WordEntry> findAll(@PathVariable UUID userId) { return service.findAll(userId); }

    @PostMapping("/{vocabularyId}")
    List<WordEntry> add(@PathVariable UUID userId, @PathVariable UUID vocabularyId) {
        return service.add(userId, vocabularyId);
    }

    @DeleteMapping("/{vocabularyId}")
    ResponseEntity<Void> remove(@PathVariable UUID userId, @PathVariable UUID vocabularyId) {
        service.remove(userId, vocabularyId);
        return ResponseEntity.noContent().build();
    }
}
