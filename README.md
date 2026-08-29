# language-api

Spring Boot backend for `language-ui`. It provides user sessions, Chinese and Spanish learning
content, HSK/CEFR levels, lessons, a persistent word catalog, and personal vocabulary lists.

## Technology

- Java 17
- Spring Boot 4.1
- Gradle 9.6 wrapper
- PostgreSQL 18
- Spring JDBC
- Springdoc OpenAPI and Swagger UI
- H2 for automated tests only

## Run locally

Start PostgreSQL using the included Compose configuration:

```bash
docker compose up -d postgres
docker compose ps
```

Start the API:

```bash
./gradlew bootRun
```

The default URLs are:

- API: `http://localhost:8000`
- Swagger UI: `http://localhost:8000/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8000/v3/api-docs`
- Health check: `http://localhost:8000/actuator/health`

Stop PostgreSQL without deleting its data:

```bash
docker compose down
```

The Compose PostgreSQL service uses a temporary in-memory filesystem. Its database is deleted when
the container stops or restarts, so the next startup begins empty and Flyway recreates the schema.
This configuration is intended for local development and must not be used for production data.

If this project previously created the old named volume, remove that unused volume once with:

```bash
docker volume rm language-api_language-api-postgres
```

## Configuration

### PostgreSQL

The application and Compose defaults match:

```text
JDBC URL: jdbc:postgresql://localhost:5432/language_api
Username: language_api
Password: language_api
```

Override them with:

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `PORT`—defaults to `8000`
- `LOG_LEVEL_ROOT`—defaults to `INFO`
- `LOG_LEVEL_APP`—defaults to `INFO` (`DEBUG` in the local profile)
- `ADMIN_EMAILS`—comma-separated emails allowed to use `/api/admin/**`
- `USERNAME_CHANGE_COOLDOWN`—minimum interval between username changes; defaults to `30d`

`DATABASE_URL` must be a JDBC URL, for example:

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/language_api
export DATABASE_USERNAME=language_api
export DATABASE_PASSWORD=change-me
```

Every HTTP response includes an `X-Request-Id`. Request logs include that ID, method, path, status,
and duration, but never log authorization headers or request bodies. Clients may supply their own
`X-Request-Id` to correlate UI and API logs.

Flyway applies versioned migrations from
[db/migration](src/main/resources/db/migration) when the application starts. The initial schema is
`V1__initial_schema.sql`; add future changes as `V2__description.sql`, `V3__description.sql`, and
so on. `baseline-on-migrate` is enabled for PostgreSQL so an existing database created by the old
`schema.sql` setup can be adopted without recreating its tables.

The `local` Spring profile is intentionally destructive: every application startup runs
`Flyway clean` followed by `Flyway migrate`, resetting all local database data. Other profiles only
run normal forward migrations.

### CORS

CORS applies to `/api/**` and is configured in `application.yml` under `app.cors`:

```yaml
app:
  cors:
    allowed-origin-patterns: ${CORS_ALLOWED_ORIGIN_PATTERNS:*}
    allowed-methods: ${CORS_ALLOWED_METHODS:GET,POST,PUT,PATCH,DELETE,OPTIONS}
    allowed-headers: ${CORS_ALLOWED_HEADERS:*}
    exposed-headers: ${CORS_EXPOSED_HEADERS:Location,Content-Type}
    allow-credentials: ${CORS_ALLOW_CREDENTIALS:true}
    max-age: ${CORS_MAX_AGE:1h}
```

For production, restrict the accepted UI origins:

```bash
export CORS_ALLOWED_ORIGIN_PATTERNS=https://language.example.com
```

Multiple values can be comma-separated.

## Authentication

Create an account and bearer session:

```http
POST /api/auth/sign-up
Content-Type: application/json
```

```json
{
  "firstName": "Maya",
  "lastName": "Chen",
  "email": "maya@example.com",
  "username": "maya.chen",
  "password": "password123"
}
```

The response includes `accessToken`. Send it to authenticated `/api/me/**` endpoints:

```http
Authorization: Bearer <accessToken>
```

Passwords are BCrypt-hashed. Accounts, bearer sessions, enrollments, lessons, and personal
vocabulary associations are currently held in memory and reset when the application restarts.
User accounts, language enrollment, the word catalog, and daily streak activity are persisted in
PostgreSQL. Passwords are stored as BCrypt hashes. Authentication bearer sessions remain in memory
and expire when the API process restarts.

## API summary

### Authentication and account

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/auth/sign-up` | Create an account and session |
| `POST` | `/api/auth/sign-in` | Sign in and create a session |
| `GET` | `/api/auth/me` | Get the bearer-token user |
| `POST` | `/api/auth/sign-out` | End the session |
| `GET` | `/api/me/profile` | Get the signed-in user's profile |
| `PATCH` | `/api/me/profile` | Edit profile details |
| `GET` | `/api/me/streak` | Get the signed-in user's streak |
| `POST` | `/api/me/streak/check-in` | Record learning activity for today and return the streak |
| `POST` | `/api/v1/users/{id}/languages/{code}` | Enroll in Chinese (`zh`) or Spanish (`es`) |

The profile API includes full `learningLanguages` objects but does not return the duplicate legacy
`languages` code set. Language enrollment is managed separately through the language endpoints.

```json
{
  "email": "maya@example.com",
  "username": "maya.chen",
  "firstName": "Maya",
  "lastName": "Chen"
}
```

### Languages and learning content

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/v1/languages` | List Chinese and Spanish |
| `GET` | `/api/v1/languages/{id}` | Get a language |
| `GET` | `/api/v1/languages/{code}/levels` | List Chinese HSK or Spanish CEFR levels |
| `GET` | `/api/v1/levels/{id}/lessons` | List lessons for a level |
| `POST` | `/api/v1/levels/{id}/lessons` | Create a lesson |
| `GET` | `/api/v1/lessons/{id}/vocabulary` | List lesson vocabulary |
| `POST` | `/api/v1/lessons/{id}/vocabulary` | Create lesson vocabulary |
| `GET` | `/api/v1/reading-lessons?language=Chinese` | List reading lessons |
| `GET` | `/api/v1/reading-lessons/{id}` | Get a passage, English translation, and aligned words |

Chinese includes HSK1–HSK6. Spanish includes CEFR A1–C2.

Reading lessons are managed through the authenticated admin API and persisted in PostgreSQL. The server calculates
each key word's `startIndex` and `endIndex` from its ordered occurrence in `originalText`.
Lessons store only curated `keyWords`, not every word in the passage. Tone-marked Chinese pinyin
uses spaces between syllables, such as `gòu wù lán`.

`GET /api/v1/reading-lessons/{id}/annotations` dynamically segments the passage using the full
word catalog and returns clickable spans with definitions. The UI should render those spans over
`originalText`; clicking one opens its returned `definition`. It can also look up selected text
directly with `GET /api/v1/words/Chinese/definitions?word=超市`.

Upload the included grocery lesson with:

`lessonType` is required and accepts `CONVERSATION`, `STORY`, `ARTICLE`, `PRACTICAL`, or
`EXPLANATION`.

### Admin lesson management

Configure one or more administrator account emails before starting the API:

```bash
export ADMIN_EMAILS=admin@example.com,editor@example.com
```

Admin requests use the bearer token returned by the normal sign-in endpoint.

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/admin/reading-lessons` | List lessons, optionally filtered by `language` |
| `GET` | `/api/admin/reading-lessons/{id}` | Get one lesson for editing |
| `POST` | `/api/admin/reading-lessons` | Upload one lesson |
| `POST` | `/api/admin/reading-lessons/bulk` | Atomically upload up to 100 lessons |
| `PUT` | `/api/admin/reading-lessons/{id}` | Replace a lesson and its keywords |
| `DELETE` | `/api/admin/reading-lessons/{id}` | Delete a lesson |

```bash
curl -X POST http://localhost:8000/api/admin/reading-lessons \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  --data-binary @data/reading/grocery-shopping-chinese.json
```

### Lesson progress

Lesson progress works with reading lessons and standard lessons. Progress is persisted in
PostgreSQL, and updating progress also records learning activity for the user's daily streak.

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/me/lesson-progress` | List the signed-in user's lesson progress |
| `GET` | `/api/me/lesson-progress?status=COMPLETED` | Filter progress by status |
| `GET` | `/api/me/lesson-progress/{lessonId}` | Get progress for one lesson |
| `PUT` | `/api/me/lesson-progress/{lessonId}` | Set progress to `STARTED` or `COMPLETED` |

```json
{
  "status": "COMPLETED"
}
```

### Word catalog

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/words/{language}` | Add one Chinese or Spanish word |
| `POST` | `/api/v1/words/{language}/bulk` | Add up to 3,000 words atomically |
| `POST` | `/api/v1/words/initialize` | Idempotently load all packaged HSK words |
| `GET` | `/api/v1/words/{language}` | Search and filter catalog words |

A word contains a list of translation senses, numeric-tone pronunciation, pinyin, level,
grammatical types, and zero or more paired examples:

```json
{
  "word": "学校",
  "englishTranslation": ["school", "CL:所[suo3]"],
  "pronunciation": "xue2 xiao4",
  "pinyin": "xué xiào",
  "level": "HSK1",
  "wordTypes": ["noun"],
  "examples": [{
    "text": "今天早上在去学校的路上，我看到了一群外国人。",
    "englishTranslation": "I saw a group of foreigners on my way to school this morning."
  }]
}
```

Chinese translation senses come from CC-CEDICT for 4,988 of 4,991 entries. The remaining three
HSK phrases retain the source list's definitions. Practical bilingual examples come from the openly
licensed Tatoeba corpus where a reliable lemma or segmented-word match exists. Entries without a
suitable example return an empty `examples` list rather than a fabricated sentence. See the
language-specific data READMEs for source and license details. The reproducible importers are
`scripts/import-cc-cedict.py` and `scripts/import-vocabulary-examples.py`.

The `GET /api/v1/words/{language}` endpoint accepts:

- `level`: `HSK1`–`HSK6` or `A1`–`C2`
- `wordType`: exact type such as `noun`, `verb`, or `adjective`
- `q`: search across the word, translation, pronunciation, and pinyin
- `offset`: records to skip; defaults to `0`
- `limit`: page size from `1` to `500`; defaults to `100`

```http
GET /api/v1/words/chinese?level=HSK1&wordType=verb&q=love&offset=0&limit=50
GET /api/v1/words/spanish?level=A1&wordType=adjective&q=good
```

### Initialize the Chinese and Spanish catalogs

The initializer loads 4,991 unique HSK1–HSK6 entries and 10,500 frequency-banded Spanish entries.
It can safely be called repeatedly because existing `(language, word, pinyin)` records are skipped.

```bash
curl -X POST http://localhost:8000/api/v1/words/initialize
```

```json
{
  "inserted": 15491,
  "skipped": 0,
  "insertedChineseWords": 4991,
  "skippedChineseWords": 0,
  "insertedSpanishWords": 10500,
  "skippedSpanishWords": 0,
  "totalChineseWords": 4991,
  "totalSpanishWords": 10500
}
```

The upload-ready source files and their attribution are documented in
[data/hsk/README.md](data/hsk/README.md).

The Spanish source, frequency-band method, and attribution are documented in
[data/spanish/README.md](data/spanish/README.md).

### Personal vocabulary

Personal vocabulary references word-catalog IDs. Users can save Chinese or Spanish words without
first enrolling in that language.

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/v1/users/{userId}/vocabulary` | List a user's saved catalog words |
| `POST` | `/api/v1/users/{userId}/vocabulary/{wordId}` | Save a catalog word |
| `DELETE` | `/api/v1/users/{userId}/vocabulary/{wordId}` | Remove a catalog word |
| `GET` | `/api/me/vocabulary?language=Chinese&status=learned` | Filter the signed-in user's words |

### Daily words

Daily-word preferences and delivery history are persisted in PostgreSQL. A user can choose from
1–20 words per day, select a language and optional level, and prevent words from repeating across
days. Repeated requests on the same day return the same words.

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/me/daily-words` | Get today's customized words |
| `GET` | `/api/me/daily-words/preferences` | Get preferences (defaults to Chinese, 1, no repeats) |
| `PUT` | `/api/me/daily-words/preferences` | Update preferences and regenerate today's selection |
| `POST` | `/api/me/daily-words/{wordId}/view` | Mark one of today's words as viewed |
| `POST` | `/api/me/daily-words/{wordId}/answer` | Record a recall result using `{"correct": true}` |
| `POST` | `/api/me/daily-words/{wordId}/complete` | Complete a word and credit the streak once the session is complete |

```json
{
  "language": "Chinese",
  "numberOfWords": 5,
  "doNotRepeat": true,
  "level": "HSK2"
}
```

Set `level` to `null` to select from every level in the chosen language.

The daily response keeps the catalog entries in `words` and includes matching `progress` entries
with `NEW`, `VIEWED`, `PRACTICING`, or `COMPLETED` status. It also reports
`requestedCount`, `deliveredCount`, `remainingNewWords`, `poolExhausted`, and
`sessionCompleted`, allowing clients to explain undersized word pools and render daily progress.

### Topics and interests

Topics group related catalog words and belong to either Chinese or Spanish. Users do not need to
be enrolled in a language to select its topics.

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/v1/topics?language=Chinese` | List topics, optionally by language |
| `GET` | `/api/v1/topics/{topicId}` | Get a topic and its word count |
| `POST` | `/api/v1/topics` | Create a topic |
| `GET` | `/api/v1/topics/{topicId}/words` | List words assigned to a topic |
| `POST` | `/api/v1/topics/{topicId}/words/{wordId}` | Assign a matching-language word |
| `DELETE` | `/api/v1/topics/{topicId}/words/{wordId}` | Remove a word from a topic |
| `GET` | `/api/me/topics` | List the signed-in user's selected topics |
| `POST` | `/api/me/topics/{topicId}` | Select a topic |
| `DELETE` | `/api/me/topics/{topicId}` | Deselect a topic |

```json
{
  "language": "Chinese",
  "name": "Food",
  "description": "Food, drinks, cooking, and dining"
}
```

### Feedback, profile, and friends

All endpoints below require an `Authorization: Bearer <token>` header.

| Method | Path | Description |
| --- | --- | --- |
| `PATCH` | `/api/me/profile` | Update the signed-in user's email, username, and names |
| `POST` | `/api/me/feedback` | Submit `GENERAL`, `FEATURE`, or `BUG` feedback |
| `GET` | `/api/me/feedback` | List the signed-in user's feedback |
| `GET` | `/api/me/friends` | List accepted friends |
| `GET` | `/api/me/friend-search?username={username}` | Find an account by exact username |
| `POST` | `/api/me/friend-requests` | Send a friend request using `{ "username": "..." }` |
| `GET` | `/api/me/friend-requests` | List incoming pending requests |
| `POST` | `/api/me/friend-requests/{requestId}/accept` | Accept a request |
| `DELETE` | `/api/me/friend-requests/{requestId}` | Decline or cancel a request |
| `DELETE` | `/api/me/friends/{friendId}` | Remove a friend |
| `GET` | `/api/me/friends/{friendId}/profile` | View an accepted friend's shared profile |
| `GET` | `/api/me/privacy` | Get friend-sharing preferences |
| `PUT` | `/api/me/privacy` | Update friend-sharing preferences |

Friend search accepts usernames only; it does not search by email, real name, or partial text.
Usernames are case-insensitive, normalized to lowercase, and can be changed once
every 30 days by default. Configure the interval with `USERNAME_CHANGE_COOLDOWN`.

Friend summaries and profiles never expose email addresses. Names and learning languages are
visible to accepted friends. Streak, completed lessons, topics, vocabulary count, and recent
activity are controlled with:

```json
{
  "shareStreak": true,
  "shareCompletedLessons": true,
  "shareTopics": true,
  "shareVocabularyCount": false,
  "shareRecentActivity": false
}
```

Feedback example:

```json
{
  "category": "FEATURE",
  "title": "Add flashcard reminders",
  "message": "Let me schedule a daily reminder for vocabulary practice."
}
```

Profile update example:

```json
{
  "email": "new-email@example.com",
  "firstName": "New",
  "lastName": "Name"
}
```

### Learn together

Accepted friends can join study groups, work through one shared reading lesson, and compare
completion progress. Invitations require consent; only the owner can invite or assign the lesson.

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/me/study-groups` | Create a group and optionally invite friends |
| `GET` | `/api/me/study-groups` | List groups joined by the user |
| `GET` | `/api/me/study-groups/{id}` | Get a group as a member |
| `POST` | `/api/me/study-groups/{id}/invitations` | Invite accepted friends as owner |
| `GET` | `/api/me/study-group-invitations` | List pending invitations |
| `POST` | `/api/me/study-group-invitations/{id}/accept` | Accept and join |
| `DELETE` | `/api/me/study-group-invitations/{id}` | Decline an invitation |
| `PUT` | `/api/me/study-groups/{id}/lesson` | Assign a shared reading lesson |
| `GET` | `/api/me/study-groups/{id}/progress` | See each member's lesson status |

```json
{
  "name": "HSK2 Grocery Crew",
  "language": "Chinese",
  "level": "HSK2",
  "friendIds": ["accepted-friend-user-id"]
}
```

Members update their own shared-lesson status through the existing
`PUT /api/me/lesson-progress/{lessonId}` API.

## Tests

Tests use Flyway with an isolated in-memory H2 database; local PostgreSQL does not need to be
running.

```bash
./gradlew test
```

## Docker deployment

Build the production image:

```bash
docker build -t language-api:latest .
```

Run it against PostgreSQL:

```bash
docker run --rm -p 8000:8000 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/language_api \
  -e DATABASE_USERNAME=language_api \
  -e DATABASE_PASSWORD=change-me \
  -e CORS_ALLOWED_ORIGIN_PATTERNS=https://language-ui.example \
  language-api:latest
```

The image uses a multi-stage Java 17 build and runs as the non-root `languageapi` user. Flyway
applies database migrations when the container starts. Configure the container platform's health
check to call `GET /actuator/health` on port `8000`.
