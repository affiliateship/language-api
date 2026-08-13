package com.languageui.api.word;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWordRequest(
        @NotBlank @Size(max = 200) String word,
        @NotBlank @Size(max = 1000) String englishTranslation,
        @NotBlank @Size(max = 200) String pronunciation,
        @Size(max = 200) String pinyin,
        @NotBlank @Size(max = 10) String level,
        @Size(max = 20) List<@NotBlank @Size(max = 50) String> wordTypes,
        @NotBlank @Size(max = 2000) String example,
        @NotBlank @Size(max = 2000) String exampleTranslation) {

    public CreateWordRequest(String word, String englishTranslation, String pronunciation,
            String pinyin, String level, List<String> wordTypes) {
        this(word, englishTranslation, pronunciation, pinyin, level, wordTypes,
                "Example using “" + word + "”.", "Example using “" + word + "”.");
    }
}
