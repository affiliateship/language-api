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
                .andExpect(jsonPath("$.user.displayName").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(response, "$.accessToken");
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.email").value("maya@example.com"));

        mockMvc.perform(get("/api/me/profile").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").doesNotExist());
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
                .andExpect(jsonPath("$.languages").doesNotExist())
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
        String secondUsername = JsonPath.read(second, "$.user.username");

        mockMvc.perform(get("/api/me/friend-search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken)
                        .queryParam("username", secondUsername.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(secondId))
                .andExpect(jsonPath("$[0].username").value(secondUsername));
        mockMvc.perform(get("/api/me/friend-search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken)
                        .queryParam("username", "second@example.com"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Updated","lastName":"User","email":"updated-%s@example.com"}
                                """.formatted(suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").doesNotExist())
                .andExpect(jsonPath("$.languages").doesNotExist())
                .andExpect(jsonPath("$.learningLanguages").isEmpty());

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
                        .content("{\"username\":\"" + secondUsername + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requester.username").isNotEmpty())
                .andExpect(jsonPath("$.requester.email").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String requestId = JsonPath.read(requestResponse, "$.id");

        mockMvc.perform(post("/api/me/friend-requests/{id}/accept", requestId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + secondToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayName").doesNotExist());

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
                .andExpect(jsonPath("$.displayName").doesNotExist())
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

    @Test
    void enforcesUniqueUsernamesAndChangeCooldown() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String response = mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Name","lastName":"Owner","email":"owner-%s@example.com",
                                 "password":"password123"}
                                """.formatted(suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(response, "$.accessToken");

        mockMvc.perform(patch("/api/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"owner-%s@example.com","username":"New_Name",
                                 "firstName":"Name","lastName":"Owner"}
                                """.formatted(suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new_name"));

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Other","lastName":"User","email":"other-%s@example.com",
                                 "username":"NEW_NAME","password":"password123"}
                                """.formatted(suffix)))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"owner-%s@example.com","username":"another_name",
                                 "firstName":"Name","lastName":"Owner"}
                                """.formatted(suffix)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.startsWith("Username can be changed again after")));
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

    private String bearer(String token) { return "Bearer " + token; }
}
