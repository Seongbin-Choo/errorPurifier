# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY gradlew gradlew.bat build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src ./src
RUN --mount=type=cache,target=/root/.gradle/caches \
    --mount=type=cache,target=/root/.gradle/wrapper \
    ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S errorpurifier \
    && adduser -S -G errorpurifier -h /app errorpurifier

WORKDIR /app
COPY --from=builder --chown=errorpurifier:errorpurifier /workspace/build/libs/error-purifier.jar ./error-purifier.jar

USER errorpurifier
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/error-purifier.jar"]
