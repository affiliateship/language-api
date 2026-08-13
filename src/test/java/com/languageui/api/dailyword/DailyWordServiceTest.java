package com.languageui.api.dailyword;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.languageui.api.user.CreateUserRequest;
import com.languageui.api.user.UserAccount;
import com.languageui.api.user.UserService;
import com.languageui.api.word.CreateWordRequest;
import com.languageui.api.word.WordCatalogRepository;
import com.languageui.api.word.WordCatalogService;
import com.languageui.api.word.WordEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DailyWordServiceTest {
    @Autowired private DailyWordService service;
    @Autowired private DailyWordRepository repository;
    @Autowired private WordCatalogService wordService;
    @Autowired private WordCatalogRepository wordRepository;
    @Autowired private UserService userService;

    @BeforeEach
    void clearData() {
        repository.deleteAll();
        wordRepository.deleteAll();
    }

    @Test
    void appliesCountLevelAndNoRepeatPreferences() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserAccount user = userService.create(new CreateUserRequest(
                "daily-" + suffix + "@example.com", "Daily", "Learner", "password123"));
        WordEntry previous = create("旧" + suffix, "old " + suffix);
        create("新一" + suffix, "new one " + suffix);
        create("新二" + suffix, "new two " + suffix);
        wordService.create("Chinese", new CreateWordRequest("高" + suffix, "advanced " + suffix,
                "gao1", "gāo", "HSK3", List.of("adjective")));

        service.update(user.id(), new UpdateDailyWordPreferencesRequest("zh", 2, true, "HSK1"));
        repository.saveDeliveries(user.id(), LocalDate.now().minusDays(1), List.of(previous));

        DailyWordsResponse first = service.dailyWords(user.id());
        DailyWordsResponse repeatedCall = service.dailyWords(user.id());

        assertThat(first.words()).hasSize(2).allMatch(word -> word.level().equals("HSK1"));
        assertThat(first.words()).doesNotContain(previous);
        assertThat(repeatedCall.words()).isEqualTo(first.words());
        assertThat(first.preferences().doNotRepeat()).isTrue();
    }

    private WordEntry create(String word, String translation) {
        return wordService.create("Chinese", new CreateWordRequest(
                word, translation, "ci2", "cí", "HSK1", List.of("noun")));
    }
}
