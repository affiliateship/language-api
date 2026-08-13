package com.languageui.api.word;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WordCatalogInitializerTest {

    @Autowired
    private WordCatalogInitializer initializer;

    @Autowired
    private WordCatalogRepository repository;

    @BeforeEach
    void clearCatalog() {
        repository.deleteAll();
    }

    @Test
    void initializesEveryPackagedChineseAndSpanishWordAndCanBeSafelyRepeated() {
        WordInitializationResponse first = initializer.initializeAllWords();
        WordInitializationResponse second = initializer.initializeAllWords();

        assertThat(first.inserted()).isEqualTo(15491);
        assertThat(first.skipped()).isZero();
        assertThat(first.insertedChineseWords()).isEqualTo(4991);
        assertThat(first.insertedSpanishWords()).isEqualTo(10500);
        assertThat(first.skippedChineseWords()).isZero();
        assertThat(first.skippedSpanishWords()).isZero();
        assertThat(first.totalChineseWords()).isEqualTo(4991);
        assertThat(first.totalSpanishWords()).isEqualTo(10500);
        assertThat(second.inserted()).isZero();
        assertThat(second.skipped()).isEqualTo(15491);
        assertThat(second.insertedChineseWords()).isZero();
        assertThat(second.insertedSpanishWords()).isZero();
        assertThat(second.skippedChineseWords()).isEqualTo(4991);
        assertThat(second.skippedSpanishWords()).isEqualTo(10500);
        assertThat(second.totalChineseWords()).isEqualTo(4991);
        assertThat(second.totalSpanishWords()).isEqualTo(10500);
    }
}
