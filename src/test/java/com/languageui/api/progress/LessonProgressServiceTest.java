package com.languageui.api.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import com.languageui.api.reading.CreateReadingLessonRequest;
import com.languageui.api.reading.CreateReadingWordRequest;
import com.languageui.api.reading.LessonType;
import com.languageui.api.reading.ReadingLesson;
import com.languageui.api.reading.ReadingLessonService;
import com.languageui.api.user.CreateUserRequest;
import com.languageui.api.user.UserAccount;
import com.languageui.api.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LessonProgressServiceTest {
    @Autowired private LessonProgressService service;
    @Autowired private LessonProgressRepository repository;
    @Autowired private ReadingLessonService readingLessonService;
    @Autowired private UserService userService;

    @BeforeEach
    void clearData() { repository.deleteAll(); }

    @Test
    void tracksStartedAndCompletedTimestampsWithoutAllowingRegression() {
        String suffix = UUID.randomUUID().toString();
        UserAccount user = userService.create(new CreateUserRequest(
                "progress-" + suffix + "@example.com", "Lesson", "Learner", "password123"));
        ReadingLesson lesson = readingLessonService.create(new CreateReadingLessonRequest(
                "Chinese", "HSK1", LessonType.STORY, "Short story " + suffix,
                "我学习。", "I study.",
                List.of(new CreateReadingWordRequest("我", "I", "wǒ"),
                        new CreateReadingWordRequest("学习", "to study", "xuéxí"))));

        LessonProgress started = service.update(user.id(), lesson.id(), LessonProgressStatus.STARTED);
        LessonProgress completed = service.update(user.id(), lesson.id(), LessonProgressStatus.COMPLETED);

        assertThat(started.startedAt()).isNotNull();
        assertThat(started.completedAt()).isNull();
        assertThat(completed.status()).isEqualTo(LessonProgressStatus.COMPLETED);
        assertThat(completed.startedAt()).isEqualTo(started.startedAt());
        assertThat(completed.completedAt()).isNotNull();
        assertThat(service.findAll(user.id(), LessonProgressStatus.COMPLETED))
                .containsExactly(completed);
        assertThatThrownBy(() -> service.update(user.id(), lesson.id(), LessonProgressStatus.STARTED))
                .isInstanceOf(IllegalStateException.class);
    }
}
