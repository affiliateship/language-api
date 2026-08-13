package com.languageui.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LanguageApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LanguageApiApplication.class, args);
    }
}
