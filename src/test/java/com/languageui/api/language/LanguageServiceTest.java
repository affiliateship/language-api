package com.languageui.api.language;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LanguageServiceTest {

    private final LanguageService service = new LanguageService();

    @Test
    void exposesOnlyChineseAndSpanish() {
        assertThat(service.findAll()).extracting(Language::code).containsExactly("zh", "es");
        assertThat(service.findByCode("ZH").name()).isEqualTo("Chinese");
        assertThatThrownBy(() -> service.create(new LanguageRequest("fr", "French")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
