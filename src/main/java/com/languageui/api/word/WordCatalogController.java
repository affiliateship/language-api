package com.languageui.api.word;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/words")
public class WordCatalogController {

    private final WordCatalogService service;
    private final WordCatalogInitializer initializer;

    public WordCatalogController(WordCatalogService service, WordCatalogInitializer initializer) {
        this.service = service;
        this.initializer = initializer;
    }

    @PostMapping("/initialize")
    WordInitializationResponse initialize() {
        return initializer.initializeAllWords();
    }

    @PostMapping("/{language}")
    ResponseEntity<WordEntry> create(
            @PathVariable String language,
            @Valid @RequestBody CreateWordRequest request) {
        WordEntry word = service.create(language, request);
        return ResponseEntity.created(URI.create("/api/v1/words/" + language + "/" + word.id())).body(word);
    }

    @PostMapping("/{language}/bulk")
    ResponseEntity<List<WordEntry>> createAll(
            @PathVariable String language,
            @Valid @RequestBody BulkWordUploadRequest request) {
        return ResponseEntity.status(201).body(service.createAll(language, request.words()));
    }

    @GetMapping("/{language}")
    List<WordEntry> findAll(
            @PathVariable String language,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String wordType,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "100") int limit) {
        return service.findAll(language, level, wordType, query, offset, limit);
    }

    @GetMapping("/{language}/definitions")
    List<WordEntry> definitions(@PathVariable String language, @RequestParam String word) {
        return service.definitions(language, word);
    }
}
