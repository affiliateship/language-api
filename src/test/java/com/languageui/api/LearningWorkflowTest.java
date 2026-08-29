package com.languageui.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.languageui.api.learning.CreateLessonRequest;
import com.languageui.api.learning.CreateVocabularyRequest;
import com.languageui.api.learning.LearningService;
import com.languageui.api.learning.Lesson;
import com.languageui.api.learning.Level;
import com.languageui.api.learning.VocabularyItem;
import com.languageui.api.user.CreateUserRequest;
import com.languageui.api.user.UserAccount;
import com.languageui.api.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LearningWorkflowTest {

    @Autowired private UserService users;
    @Autowired private LearningService learning;

    @Test
    void userEnrollsAndSavesLessonVocabulary() {
        UserAccount user = users.create(new CreateUserRequest(
                "learner-" + java.util.UUID.randomUUID() + "@example.com",
                "Language", "Learner", "password123"));
        user = users.addLanguage(user.id(), "zh");
        Level hsk1 = learning.levels("zh").get(0);
        Lesson lesson = learning.createLesson(hsk1.id(), new CreateLessonRequest("Greetings", "Basics", 1));
        VocabularyItem item = learning.createVocabulary(lesson.id(),
                new CreateVocabularyRequest("你好", "hello", "nǐ hǎo"));

        assertThat(user.languageCodes()).containsExactly("zh");
        assertThat(item.term()).isEqualTo("你好");
    }
}
