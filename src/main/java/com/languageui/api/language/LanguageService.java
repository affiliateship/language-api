package com.languageui.api.language;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class LanguageService {

    private final Map<UUID, Language> languages = new ConcurrentHashMap<>();

    public LanguageService() {
        addCatalogLanguage("zh", "Chinese");
        addCatalogLanguage("es", "Spanish");
    }

    public List<Language> findAll() {
        return languages.values().stream()
                .sorted(Comparator.comparing(Language::name))
                .toList();
    }

    public Language findById(UUID id) {
        Language language = languages.get(id);
        if (language == null) {
            throw new LanguageNotFoundException(id);
        }
        return language;
    }

    public Language create(LanguageRequest request) {
        String code = normalizeCode(request.code());
        if (!code.equals("zh") && !code.equals("es")) {
            throw new IllegalArgumentException("Only Chinese (zh) and Spanish (es) are supported");
        }
        if (languages.values().stream().anyMatch(language -> language.code().equals(code))) {
            throw new IllegalStateException("Language code already exists: " + code);
        }
        Language language = new Language(UUID.randomUUID(), code, request.name().trim());
        languages.put(language.id(), language);
        return language;
    }

    public Language update(UUID id, LanguageRequest request) {
        findById(id);
        Language language = new Language(id, normalizeCode(request.code()), request.name().trim());
        languages.put(id, language);
        return language;
    }

    public void delete(UUID id) {
        if (languages.remove(id) == null) {
            throw new LanguageNotFoundException(id);
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toLowerCase();
    }

    public Language findByCode(String code) {
        return languages.values().stream()
                .filter(language -> language.code().equals(normalizeCode(code)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported language: " + code));
    }

    private void addCatalogLanguage(String code, String name) {
        Language language = new Language(UUID.nameUUIDFromBytes(code.getBytes()), code, name);
        languages.put(language.id(), language);
    }
}
