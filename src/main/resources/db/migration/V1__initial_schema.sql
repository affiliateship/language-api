-- Initial language-api schema managed by Flyway.
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    username VARCHAR(30) NOT NULL,
    username_changed_at TIMESTAMP,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS user_languages (
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    enrolled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, language_code)
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

CREATE TABLE IF NOT EXISTS word_entries (
    id VARCHAR(36) PRIMARY KEY,
    language VARCHAR(20) NOT NULL,
    word VARCHAR(200) NOT NULL,
    english_translations VARCHAR(12000) NOT NULL,
    pronunciation VARCHAR(200) NOT NULL,
    pinyin VARCHAR(200) NOT NULL,
    level VARCHAR(10) NOT NULL,
    word_types VARCHAR(1000) NOT NULL,
    examples VARCHAR(24000) NOT NULL,
    CONSTRAINT uq_word_entry UNIQUE (language, word, pinyin)
);

CREATE INDEX IF NOT EXISTS idx_word_language_level ON word_entries(language, level);
CREATE INDEX IF NOT EXISTS idx_word_language_word ON word_entries(language, word);

CREATE TABLE IF NOT EXISTS topics (
    id VARCHAR(36) PRIMARY KEY,
    language VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    CONSTRAINT uq_topic_language_name UNIQUE (language, name)
);

CREATE TABLE IF NOT EXISTS topic_words (
    topic_id VARCHAR(36) NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    word_id VARCHAR(36) NOT NULL REFERENCES word_entries(id) ON DELETE CASCADE,
    PRIMARY KEY (topic_id, word_id)
);

CREATE TABLE IF NOT EXISTS user_topics (
    user_id VARCHAR(36) NOT NULL,
    topic_id VARCHAR(36) NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    selected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, topic_id)
);

CREATE INDEX IF NOT EXISTS idx_topic_language ON topics(language);
CREATE INDEX IF NOT EXISTS idx_topic_words_word ON topic_words(word_id);
CREATE INDEX IF NOT EXISTS idx_user_topics_user ON user_topics(user_id);

CREATE TABLE IF NOT EXISTS feedback (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    category VARCHAR(20) NOT NULL,
    title VARCHAR(120) NOT NULL,
    message VARCHAR(4000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS friend_requests (
    id VARCHAR(36) PRIMARY KEY,
    requester_id VARCHAR(36) NOT NULL,
    recipient_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_friend_request UNIQUE (requester_id, recipient_id)
);

CREATE TABLE IF NOT EXISTS friendships (
    user_id VARCHAR(36) NOT NULL,
    friend_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, friend_id)
);

CREATE INDEX IF NOT EXISTS idx_feedback_user ON feedback(user_id);
CREATE INDEX IF NOT EXISTS idx_friend_requests_recipient ON friend_requests(recipient_id, status);
CREATE INDEX IF NOT EXISTS idx_friendships_user ON friendships(user_id);

CREATE TABLE IF NOT EXISTS user_learning_activity (
    user_id VARCHAR(36) NOT NULL,
    activity_date DATE NOT NULL,
    activity_count INTEGER NOT NULL DEFAULT 1,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, activity_date)
);

CREATE INDEX IF NOT EXISTS idx_learning_activity_user_date
    ON user_learning_activity(user_id, activity_date);

CREATE TABLE IF NOT EXISTS reading_lessons (
    id VARCHAR(36) PRIMARY KEY,
    language VARCHAR(20) NOT NULL,
    level VARCHAR(10) NOT NULL,
    lesson_type VARCHAR(20) NOT NULL DEFAULT 'ARTICLE',
    title VARCHAR(150) NOT NULL,
    original_text VARCHAR(8000) NOT NULL,
    english_translation VARCHAR(8000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reading_words (
    id VARCHAR(36) PRIMARY KEY,
    lesson_id VARCHAR(36) NOT NULL REFERENCES reading_lessons(id) ON DELETE CASCADE,
    sequence_number INTEGER NOT NULL,
    original VARCHAR(200) NOT NULL,
    english_translation VARCHAR(1000) NOT NULL,
    pronunciation VARCHAR(200) NOT NULL,
    start_index INTEGER NOT NULL,
    end_index INTEGER NOT NULL,
    CONSTRAINT uq_reading_word_sequence UNIQUE (lesson_id, sequence_number)
);

CREATE INDEX IF NOT EXISTS idx_reading_lesson_language ON reading_lessons(language);
CREATE INDEX IF NOT EXISTS idx_reading_words_lesson ON reading_words(lesson_id, sequence_number);

CREATE TABLE IF NOT EXISTS daily_word_preferences (
    user_id VARCHAR(36) PRIMARY KEY,
    language VARCHAR(20) NOT NULL,
    number_of_words INTEGER NOT NULL,
    do_not_repeat BOOLEAN NOT NULL,
    level VARCHAR(10),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS daily_word_deliveries (
    user_id VARCHAR(36) NOT NULL,
    delivery_date DATE NOT NULL,
    word_id VARCHAR(36) NOT NULL REFERENCES word_entries(id) ON DELETE CASCADE,
    sequence_number INTEGER NOT NULL,
    viewed_at TIMESTAMP,
    answer_count INTEGER NOT NULL DEFAULT 0,
    correct_answer_count INTEGER NOT NULL DEFAULT 0,
    completed_at TIMESTAMP,
    PRIMARY KEY (user_id, delivery_date, word_id),
    CONSTRAINT uq_daily_word_sequence UNIQUE (user_id, delivery_date, sequence_number)
);

CREATE INDEX IF NOT EXISTS idx_daily_word_history ON daily_word_deliveries(user_id, word_id);

CREATE TABLE IF NOT EXISTS lesson_progress (
    user_id VARCHAR(36) NOT NULL,
    lesson_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, lesson_id)
);

CREATE INDEX IF NOT EXISTS idx_lesson_progress_user_status
    ON lesson_progress(user_id, status);

CREATE TABLE IF NOT EXISTS friend_privacy_settings (
    user_id VARCHAR(36) PRIMARY KEY,
    share_streak BOOLEAN NOT NULL DEFAULT TRUE,
    share_completed_lessons BOOLEAN NOT NULL DEFAULT TRUE,
    share_topics BOOLEAN NOT NULL DEFAULT TRUE,
    share_vocabulary_count BOOLEAN NOT NULL DEFAULT FALSE,
    share_recent_activity BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS study_groups (
    id VARCHAR(36) PRIMARY KEY,
    owner_id VARCHAR(36) NOT NULL,
    name VARCHAR(120) NOT NULL,
    language VARCHAR(20) NOT NULL,
    level VARCHAR(10),
    lesson_id VARCHAR(36),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS study_group_members (
    group_id VARCHAR(36) NOT NULL REFERENCES study_groups(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, user_id)
);

CREATE TABLE IF NOT EXISTS study_group_invitations (
    id VARCHAR(36) PRIMARY KEY,
    group_id VARCHAR(36) NOT NULL REFERENCES study_groups(id) ON DELETE CASCADE,
    inviter_id VARCHAR(36) NOT NULL,
    invitee_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_study_group_invitee UNIQUE (group_id, invitee_id)
);

CREATE INDEX IF NOT EXISTS idx_study_group_members_user ON study_group_members(user_id);
CREATE INDEX IF NOT EXISTS idx_study_group_invites_user ON study_group_invitations(invitee_id, status);
