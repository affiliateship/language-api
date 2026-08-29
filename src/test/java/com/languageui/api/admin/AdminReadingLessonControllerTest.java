package com.languageui.api.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.languageui.api.reading.ReadingLessonRepository;
import com.languageui.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AdminReadingLessonControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ReadingLessonRepository repository;
    @Autowired UserRepository userRepository;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        repository.deleteAll();
        userRepository.deleteAll();
        adminToken = token(signUp("Admin", "admin@example.com"));
        userToken = token(signUp("Learner", "learner@example.com"));
    }

    @Test
    void requiresAnAuthenticatedConfiguredAdministrator() throws Exception {
        mockMvc.perform(get("/api/admin/reading-lessons"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/reading-lessons")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void supportsBulkUploadListingUpdateAndDelete() throws Exception {
        String response = mockMvc.perform(post("/api/admin/reading-lessons/bulk")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lessons":[{
                                  "language":"Chinese","level":"HSK1","lessonType":"PRACTICAL",
                                  "title":"At the shop","originalText":"我去超市。",
                                  "englishTranslation":"I am going to the supermarket.",
                                  "keyWords":[{"original":"超市","englishTranslation":"supermarket",
                                    "pronunciation":"chao1 shi4"}]
                                }]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].language").value("Chinese"))
                .andExpect(jsonPath("$[0].keyWords[0].startIndex").value(2))
                .andReturn().getResponse().getContentAsString();
        String lessonId = JsonPath.read(response, "$[0].id");

        mockMvc.perform(get("/api/admin/reading-lessons")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .queryParam("language", "zh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("At the shop"));

        mockMvc.perform(put("/api/admin/reading-lessons/{id}", lessonId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"Spanish","level":"A1","lessonType":"ARTICLE",
                                  "title":"At the market","originalText":"Voy al mercado.",
                                  "englishTranslation":"I am going to the market.",
                                  "keyWords":[{"original":"mercado","englishTranslation":"market",
                                    "pronunciation":"mercado"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("Spanish"))
                .andExpect(jsonPath("$.keyWords[0].startIndex").value(7));

        mockMvc.perform(delete("/api/admin/reading-lessons/{id}", lessonId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/reading-lessons/{id}", lessonId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void supportsSingleLessonUpload() throws Exception {
        mockMvc.perform(post("/api/admin/reading-lessons")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"language":"Spanish","level":"A1","lessonType":"PRACTICAL",
                                  "title":"Greeting","originalText":"Hola, Ana.",
                                  "englishTranslation":"Hello, Ana.",
                                  "keyWords":[{"original":"Hola","englishTranslation":"Hello",
                                    "pronunciation":"Hola"}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION,
                        org.hamcrest.Matchers.startsWith("/api/admin/reading-lessons/")));
    }

    private String signUp(String firstName, String email) throws Exception {
        return mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"%s","lastName":"User","email":"%s",
                                  "password":"password123"}
                                """.formatted(firstName, email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String token(String response) { return JsonPath.read(response, "$.accessToken"); }
    private String bearer(String token) { return "Bearer " + token; }
}
