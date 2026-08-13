package com.languageui.api.topic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import com.languageui.api.language.LanguageService;
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
class TopicServiceTest {

    @Autowired private TopicService topicService;
    @Autowired private TopicRepository topicRepository;
    @Autowired private WordCatalogService wordCatalogService;
    @Autowired private WordCatalogRepository wordCatalogRepository;
    @Autowired private UserService userService;

    @BeforeEach
    void clearData() {
        topicRepository.deleteAll();
        wordCatalogRepository.deleteAll();
    }

    @Test
    void groupsWordsAndAllowsUserToSelectTopic() {
        WordEntry word = wordCatalogService.create("Chinese",
                new CreateWordRequest("苹果", "apple", "ping2 guo3", "píng guǒ", "HSK1", List.of("noun")));
        Topic topic = topicService.create(new CreateTopicRequest("zh", "Food", "Food and dining"));
        topic = topicService.addWord(topic.id(), word.id());
        UserAccount user = userService.create(
                new CreateUserRequest("topic-" + UUID.randomUUID() + "@example.com",
                        "Topic", "User", "password123"));

        assertThat(topic.wordCount()).isEqualTo(1);
        assertThat(topicService.words(topic.id())).containsExactly(word);
        assertThat(topicService.select(user.id(), topic.id())).containsExactly(topic);
    }

    @Test
    void rejectsWordFromAnotherLanguage() {
        WordEntry spanish = wordCatalogService.create("Spanish",
                new CreateWordRequest("pan", "bread", "pan", null, "A1", List.of("noun")));
        Topic chinese = topicService.create(new CreateTopicRequest("Chinese", "Travel", ""));

        assertThatThrownBy(() -> topicService.addWord(chinese.id(), spanish.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("languages must match");
    }
}
