# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle* settings.gradle* ./

RUN chmod +x gradlew

COPY src src

RUN ./gradlew clean bootJar \
    --no-daemon \
    -x test \
    && JAR_FILE="$(find build/libs \
        -maxdepth 1 \
        -type f \
        -name '*.jar' \
        ! -name '*-plain.jar' \
        | head -n 1)" \
    && test -n "${JAR_FILE}" \
    && cp "${JAR_FILE}" /workspace/app.jar


FROM eclipse-temurin:25-jre-jammy

WORKDIR /app

RUN groupadd --system spring \
    && useradd --system \
        --gid spring \
        --home-dir /app \
        spring

COPY --from=builder /workspace/app.jar /app/app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]