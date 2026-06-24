# Stage 1: Extract the pre-built boot jar into layers.
# The jar must already exist in build/libs (host/CI: ./gradlew bootJar).
FROM eclipse-temurin:25-jre-alpine AS builder
WORKDIR /builder

# Exactly one jar thanks to `jar { enabled = false }`; rename to a stable name.
COPY build/libs/*.jar application.jar

RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# Stage 2: Runtime image assembled from the extracted layers.
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

# Least-changing -> most-changing, so a code-only change reuses cached layers.
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./

EXPOSE 8080
# Thin jar produced by `extract --layers`; resolves deps from sibling dirs.
ENTRYPOINT ["java", "-jar", "application.jar"]
