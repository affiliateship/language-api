# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

COPY src ./src
COPY data ./data
RUN ./gradlew --no-daemon clean bootJar -x test

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

RUN groupadd --system languageapi \
    && useradd --system --gid languageapi --home-dir /app --shell /usr/sbin/nologin languageapi
COPY --from=build --chown=languageapi:languageapi \
    /workspace/build/libs/language-api-0.0.1-SNAPSHOT.jar /app/language-api.jar

USER languageapi
EXPOSE 8000

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["java", "-jar", "/app/language-api.jar"]
