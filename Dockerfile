# syntax=docker/dockerfile:1
# ---------- build stage: compile, test-compile and package with the Maven wrapper ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
# Download dependencies in their own layer so code changes don't re-download the internet.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline
COPY src src
COPY README.md Dockerfile docker-compose.yml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests

# ---------- runtime stage: JRE only, non-root user ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --uid 1001 app
COPY --from=build /workspace/target/playground-*.jar app.jar
USER app
EXPOSE 8080
# Size the heap from the container's memory limit instead of the host's.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
