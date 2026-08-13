package com.languageui.api.reading;

import java.util.List;
import java.util.UUID;

public record ReadingLesson(UUID id, String language, String level, LessonType lessonType, String title,
                            String originalText, String englishTranslation,
                            List<ReadingWord> keyWords) {
}
