package com.languageui.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI languageApi() {
        return new OpenAPI()
                .servers(List.of(new Server()
                        .url("/")
                        .description("Current host")))
                .info(new Info()
                        .title("Language API")
                        .version("v1")
                        .description(
                                "Accounts, language enrollment, lessons, levels, and vocabulary for language-ui."));
    }
}
