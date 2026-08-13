ALTER TABLE word_entries ADD COLUMN example VARCHAR(2000);
ALTER TABLE word_entries ADD COLUMN example_translation VARCHAR(2000);

UPDATE word_entries
SET example = CASE
        WHEN language = 'Chinese' THEN '我正在学习“' || word || '”这个词。'
        ELSE 'Estoy aprendiendo la palabra «' || word || '».'
    END,
    example_translation = 'I am learning the word “' || word || '”.';

ALTER TABLE word_entries ALTER COLUMN example SET NOT NULL;
ALTER TABLE word_entries ALTER COLUMN example_translation SET NOT NULL;
