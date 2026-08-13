package com.languageui.api.word;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class WordCatalogInitializer {

    private final WordCatalogService wordCatalogService;
    private final ResourceLoader resourceLoader;
    private final JsonMapper jsonMapper;

    public WordCatalogInitializer(WordCatalogService wordCatalogService, ResourceLoader resourceLoader,
            JsonMapper jsonMapper) {
        this.wordCatalogService = wordCatalogService;
        this.resourceLoader = resourceLoader;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public WordInitializationResponse initializeAllWords() {
        int insertedChinese = 0;
        int skippedChinese = 0;
        for (int level = 1; level <= 6; level++) {
            BulkWordUploadRequest upload = read("classpath:hsk/hsk" + level + ".json", "HSK" + level);
            for (CreateWordRequest word : upload.words()) {
                if (wordCatalogService.createIfAbsent("Chinese", word)) insertedChinese++;
                else skippedChinese++;
            }
        }
        int insertedSpanish = 0;
        int skippedSpanish = 0;
        for (String level : new String[] {"a1", "a2", "b1", "b2", "c1", "c2"}) {
            BulkWordUploadRequest upload = read("classpath:spanish/" + level + ".json", level.toUpperCase());
            for (CreateWordRequest word : upload.words()) {
                if (wordCatalogService.createIfAbsent("Spanish", word)) insertedSpanish++;
                else skippedSpanish++;
            }
        }
        return new WordInitializationResponse(insertedChinese + insertedSpanish,
                skippedChinese + skippedSpanish, insertedChinese, skippedChinese,
                insertedSpanish, skippedSpanish,
                wordCatalogService.count("Chinese"), wordCatalogService.count("Spanish"));
    }

    private BulkWordUploadRequest read(String location, String label) {
        Resource resource = resourceLoader.getResource(location);
        try (var input = resource.getInputStream()) {
            return jsonMapper.readValue(input, BulkWordUploadRequest.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read " + label + " vocabulary", exception);
        }
    }
}
