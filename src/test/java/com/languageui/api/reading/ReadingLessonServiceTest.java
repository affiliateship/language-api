package com.languageui.api.reading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.languageui.api.word.CreateWordRequest;
import com.languageui.api.word.WordCatalogRepository;
import com.languageui.api.word.WordCatalogService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ReadingLessonServiceTest {
    @Autowired private ReadingLessonService service;
    @Autowired private ReadingLessonRepository repository;
    @Autowired private WordCatalogService wordCatalogService;
    @Autowired private WordCatalogRepository wordCatalogRepository;

    @BeforeEach
    void clearData() {
        repository.deleteAll();
        wordCatalogRepository.deleteAll();
    }

    @Test
    void createsAndPersistsAReadingLessonWithOriginalWordAssociations() {
        ReadingLesson lesson = service.create(new CreateReadingLessonRequest(
                "zh", "HSK2", LessonType.PRACTICAL, "Grocery Shopping", "我去超市买苹果。",
                "I go to the supermarket to buy apples.", List.of(
                        word("我", "I", "wǒ"), word("去", "to go", "qù"),
                        word("超市", "supermarket", "chāoshì"),
                        word("买", "to buy", "mǎi"), word("苹果", "apple", "píngguǒ"))));

        ReadingLesson stored = service.findById(lesson.id());
        assertThat(stored.keyWords()).hasSize(5).allSatisfy(word ->
                assertThat(stored.originalText().substring(word.startIndex(), word.endIndex()))
                        .isEqualTo(word.original()));
        assertThat(stored.lessonType()).isEqualTo(LessonType.PRACTICAL);
        assertThat(service.findAll("Chinese")).containsExactly(stored);
    }

    @Test
    void rejectsWordsThatCannotBeAssociatedInOrder() {
        assertThatThrownBy(() -> service.create(new CreateReadingLessonRequest(
                "Chinese", "HSK2", LessonType.STORY, "Invalid", "我买苹果。", "I buy apples.",
                List.of(word("苹果", "apple", "píngguǒ"), word("我", "I", "wǒ")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing or out of order");
    }

    @Test
    void derivesClickableDefinitionsBeyondTheStoredKeyWords() {
        wordCatalogService.create("Chinese", new CreateWordRequest(
                "我", "I; me", "wo3", "wǒ", "HSK1", List.of("pronoun")));
        wordCatalogService.create("Chinese", new CreateWordRequest(
                "超市", "supermarket", "chao1 shi4", "chāo shì", "HSK1", List.of("noun")));
        ReadingLesson lesson = service.create(new CreateReadingLessonRequest(
                "Chinese", "HSK1", LessonType.PRACTICAL, "Shopping", "我去超市。",
                "I go to the supermarket.", List.of(word("超市", "supermarket", "chāo shì"))));

        assertThat(service.annotations(lesson.id()))
                .extracting(annotation -> annotation.definition().word())
                .containsExactly("我", "超市");
        assertThat(lesson.keyWords()).extracting(ReadingWord::original).containsExactly("超市");
    }

    private CreateReadingWordRequest word(String original, String translation, String pronunciation) {
        return new CreateReadingWordRequest(original, translation, pronunciation);
    }
}
