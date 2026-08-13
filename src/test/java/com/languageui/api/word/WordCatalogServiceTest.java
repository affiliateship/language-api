package com.languageui.api.word;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WordCatalogServiceTest {

    @Autowired
    private WordCatalogService service;

    @Autowired
    private WordCatalogRepository repository;

    @BeforeEach
    void clearCatalog() {
        repository.deleteAll();
    }

    @Test
    void uploadsChineseAndSpanishWords() {
        WordEntry chinese = service.create("chinese",
                new CreateWordRequest("你好", "hello", "nǐ hǎo", "nǐ hǎo", "HSK 1", List.of("Greeting")));
        WordEntry spanish = service.create("spanish",
                new CreateWordRequest("hola", "hello", "OH-lah", null, "A1", List.of("Interjection")));

        assertThat(chinese.level()).isEqualTo("HSK1");
        assertThat(chinese.pinyin()).isEqualTo("nǐ hǎo");
        assertThat(chinese.wordTypes()).containsExactly("greeting");
        assertThat(spanish.language()).isEqualTo("Spanish");
        assertThat(service.findAll("zh", "HSK1")).containsExactly(chinese);
    }

    @Test
    void rejectsInvalidLevelsBeforeStoringBulkUpload() {
        List<CreateWordRequest> batch = List.of(
                new CreateWordRequest("你", "you", "nǐ", "nǐ", "HSK1", List.of("pronoun")),
                new CreateWordRequest("词", "word", "cí", "cí", "HSK9", List.of("noun")));

        assertThatThrownBy(() -> service.createAll("Chinese", batch))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(service.findAll("Chinese", null)).isEmpty();
    }

    @Test
    void filtersWordsByLevelTypeAndSearchText() {
        service.create("chinese", new CreateWordRequest(
                "好", "good", "hao3", "hǎo", "HSK1", List.of("adjective")));
        service.create("chinese", new CreateWordRequest(
                "你好", "hello", "ni3 hao3", "nǐ hǎo", "HSK1", List.of("greeting")));
        service.create("spanish", new CreateWordRequest(
                "bueno", "good", "BWEH-noh", null, "A1", List.of("adjective")));

        assertThat(service.findAll("zh", "HSK1", "adjective", "good", 0, 20))
                .extracting(WordEntry::word)
                .containsExactly("好");
        assertThat(service.findAll("Chinese", null, null, "nǐ", 0, 20))
                .extracting(WordEntry::word)
                .containsExactly("你好");
    }
}
