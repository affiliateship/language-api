package com.languageui.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

    @Test
    void usesTheCurrentHostForSwaggerRequests() {
        OpenAPI openApi = new OpenApiConfig().languageApi();

        assertThat(openApi.getServers()).hasSize(1);
        assertThat(openApi.getServers().get(0).getUrl()).isEqualTo("/");
    }
}
