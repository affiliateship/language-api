package com.languageui.api.dailyword;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.languageui.api.user.CreateUserRequest;
import com.languageui.api.user.UserAccount;
import com.languageui.api.user.UserService;
import com.languageui.api.streak.StreakService;
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
    @Autowired private StreakService streakService;

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
        assertThat(first.requestedCount()).isEqualTo(2);
        assertThat(first.deliveredCount()).isEqualTo(2);
        assertThat(first.progress()).allMatch(item -> item.status() == DailyWordStatus.NEW);
    }

    @Test
    void tracksPracticeAndCreditsStreakWhenTheWholeSessionIsCompleted() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserAccount user = userService.create(new CreateUserRequest(
                "progress-" + suffix + "@example.com", "Daily", "Learner", "password123"));
        create("甲" + suffix, "first " + suffix);
        create("乙" + suffix, "second " + suffix);
        service.update(user.id(), new UpdateDailyWordPreferencesRequest("Chinese", 2, true, "HSK1"));

        DailyWordsResponse daily = service.dailyWords(user.id());
        UUID first = daily.words().get(0).id();
        UUID second = daily.words().get(1).id();

        assertThat(service.markViewed(user.id(), first).status()).isEqualTo(DailyWordStatus.VIEWED);
        service.answer(user.id(), first, false);
        DailyWordProgress practiced = service.answer(user.id(), first, true);
        assertThat(practiced.status()).isEqualTo(DailyWordStatus.PRACTICING);
        assertThat(practiced.answerCount()).isEqualTo(2);
        assertThat(practiced.correctAnswerCount()).isEqualTo(1);

        DailyWordsResponse partial = service.complete(user.id(), first);
        assertThat(partial.sessionCompleted()).isFalse();
        assertThat(streakService.get(user.id()).currentDays()).isZero();

        DailyWordsResponse completed = service.complete(user.id(), second);
        assertThat(completed.sessionCompleted()).isTrue();
        assertThat(completed.progress()).allMatch(item -> item.status() == DailyWordStatus.COMPLETED);
        assertThat(streakService.get(user.id()).currentDays()).isEqualTo(1);

        service.complete(user.id(), second);
        assertThat(streakService.get(user.id()).currentDays()).isEqualTo(1);
    }

    @Test
    void reportsWhenThereAreNotEnoughUnseenWords() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UserAccount user = userService.create(new CreateUserRequest(
                "pool-" + suffix + "@example.com", "Daily", "Learner", "password123"));
        create("仅" + suffix, "only " + suffix);
        service.update(user.id(), new UpdateDailyWordPreferencesRequest("Chinese", 3, true, "HSK1"));

        DailyWordsResponse response = service.dailyWords(user.id());

        assertThat(response.requestedCount()).isEqualTo(3);
        assertThat(response.deliveredCount()).isEqualTo(1);
        assertThat(response.remainingNewWords()).isZero();
        assertThat(response.poolExhausted()).isTrue();
    }

    private WordEntry create(String word, String translation) {
        return wordService.create("Chinese", new CreateWordRequest(
                word, translation, "ci2", "cí", "HSK1", List.of("noun")));
    }
}
