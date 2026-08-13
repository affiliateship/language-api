package com.languageui.api.language;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/languages")
public class LanguageController {

    private final LanguageService languageService;

    public LanguageController(LanguageService languageService) {
        this.languageService = languageService;
    }

    @GetMapping
    public List<Language> findAll() {
        return languageService.findAll();
    }

    @GetMapping("/{id}")
    public Language findById(@PathVariable UUID id) {
        return languageService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Language> create(@Valid @RequestBody LanguageRequest request) {
        Language language = languageService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/languages/" + language.id())).body(language);
    }

    @PutMapping("/{id}")
    public Language update(@PathVariable UUID id, @Valid @RequestBody LanguageRequest request) {
        return languageService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        languageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
