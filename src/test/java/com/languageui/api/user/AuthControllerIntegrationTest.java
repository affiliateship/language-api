package com.languageui.api.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.languageui.api.vocabulary.PersonalVocabularyService;
import com.languageui.api.word.CreateWordRequest;
import com.languageui.api.word.WordCatalogService;
import com.languageui.api.word.WordEntry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WordCatalogService wordCatalogService;

    @Autowired
    private PersonalVocabularyService personalVocabularyService;

    @Test
    void supportsUiAuthenticationWorkflow() throws Exception {
        String response = mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Maya","lastName":"Chen","email":"maya@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.displayName").value("Maya Chen"))
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(response, "$.accessToken");
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.email").value("maya@example.com"));

        mockMvc.perform(get("/api/me/profile").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Maya Chen"));
        mockMvc.perform(get("/api/me/streak").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentDays").value(0));
        mockMvc.perform(post("/api/me/streak/check-in")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentDays").value(1))
                .andExpect(jsonPath("$.longestDays").value(1))
                .andExpect(jsonPath("$.lastActivityDate").isNotEmpty());

        String userId = JsonPath.read(response, "$.user.id");
        java.util.UUID accountId = java.util.UUID.fromString(userId);
        mockMvc.perform(post("/api/v1/users/{id}/languages/zh", userId))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/me/profile").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languages[0]").value("zh"))
                .andExpect(jsonPath("$.learningLanguages[0].code").value("zh"))
                .andExpect(jsonPath("$.learningLanguages[0].name").value("Chinese"));
        WordEntry item = wordCatalogService.create("Chinese",
                new CreateWordRequest("您好", "hello", "nin2 hao3", "nín hǎo", "HSK1", List.of("greeting")));
        personalVocabularyService.add(accountId, item.id());

        mockMvc.perform(get("/api/me/vocabulary")
                        .queryParam("language", "Chinese")
                        .queryParam("status", "learned")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].language").value("Chinese"))
                .andExpect(jsonPath("$[0].status").value("learned"))
                .andExpect(jsonPath("$[0].term").value("您好"))
                .andExpect(jsonPath("$[0].level").value("HSK1"))
                .andExpect(jsonPath("$[0].wordTypes[0]").value("greeting"));
    }

    @Test
    void allowsCredentialedPreflightFromAnyOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/sign-up")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void allowsPreflightForWordInitializationApi() throws Exception {
        mockMvc.perform(options("/api/v1/words/initialize")
                        .header(HttpHeaders.ORIGIN, "https://language-ui.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://language-ui.example"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));
    }

    @Test
    void supportsProfileFeedbackAndFriendWorkflow() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String first = signUp("First User", "first-" + suffix + "@example.com");
        String second = signUp("Second User", "second-" + suffix + "@example.com");
        String firstToken = JsonPath.read(first, "$.accessToken");
        String secondToken = JsonPath.read(second, "$.accessToken");
        String secondId = JsonPath.read(second, "$.user.id");

        mockMvc.perform(patch("/api/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Updated","lastName":"User","email":"updated-%s@example.com",
                                 "learningLanguageCodes":["zh","es"]}
                                """.formatted(suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated User"))
                .andExpect(jsonPath("$.learningLanguages.length()").value(2))
                .andExpect(jsonPath("$.learningLanguages[0].code").value("zh"))
                .andExpect(jsonPath("$.learningLanguages[1].code").value("es"));

        mockMvc.perform(post("/api/me/feedback")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"BUG","title":"Audio issue","message":"Audio does not play"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        String requestResponse = mockMvc.perform(post("/api/me/friend-requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + secondId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requester.displayName").value("Updated User"))
                .andExpect(jsonPath("$.requester.email").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String requestId = JsonPath.read(requestResponse, "$.id");

        mockMvc.perform(post("/api/me/friend-requests/{id}/accept", requestId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + secondToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayName").value("Updated User"));

        mockMvc.perform(put("/api/me/privacy")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + secondToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shareStreak":false,"shareCompletedLessons":true,
                                 "shareTopics":false,"shareVocabularyCount":true,
                                 "shareRecentActivity":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareStreak").value(false));

        mockMvc.perform(get("/api/me/friends/{id}/profile", secondId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Second User"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.completedLessons").value(0))
                .andExpect(jsonPath("$.vocabularyCount").value(0));

        mockMvc.perform(delete("/api/me/friends/{id}", secondId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/me/friends/{id}/profile", secondId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken))
                .andExpect(status().isForbidden());
    }

    private String signUp(String name, String email) throws Exception {
        return mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"%s","lastName":"User","email":"%s","password":"password123"}
                                """.formatted(name.split(" ")[0], email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
