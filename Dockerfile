# Stage 1: Build
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Download dependencies first (cached layer)
RUN ./gradlew dependencies --no-daemon -q

COPY src src
RUN ./gradlew bootJar --no-daemon -q

# Stage 2: Runtime
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
