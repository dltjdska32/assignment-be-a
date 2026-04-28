
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x gradlew \
    && ./gradlew bootJar --no-daemon -x test \
    && JAR=$(find build/libs -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' | head -n1) \
    && test -n "$JAR" \
    && cp "$JAR" /app/application.jar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=builder /app/application.jar app.jar
USER spring:spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
