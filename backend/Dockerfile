# Backend image: built with the Gradle wrapper, run on a slim JRE as a non-root user.

# --- Build ---------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Dependencies first, so they are cached until the build files change.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null

COPY src src
# The tests run in CI (they need Docker for Testcontainers), not inside the image build.
RUN ./gradlew --no-daemon bootJar -x test && cp build/libs/*.jar app.jar

# --- Run -----------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre
RUN groupadd --system pokedex && useradd --system --gid pokedex --no-create-home pokedex
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
USER pokedex

EXPOSE 8080
# Let the JVM size its memory from the container's limit.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
