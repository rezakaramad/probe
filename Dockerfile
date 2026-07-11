# syntax=docker/dockerfile:1
# -------------------------------------------------
# Runtime-only image.
#
# The JAR is built AND tested in CI (`./gradlew build`) and passed in via the
# JAR_FILE build-arg, so the image ships exactly the artifact that passed tests.
#
# Tests are intentionally NOT run here: the integration test uses Testcontainers,
# which needs a live Docker daemon at test time. That daemon exists on the CI
# runner but not inside a `docker build`, so building the jar in-image could
# never run those tests.
# -------------------------------------------------
FROM gcr.io/distroless/java21-debian12:nonroot

WORKDIR /app

# Path (relative to the build context) of the pre-built Spring Boot jar.
ARG JAR_FILE=app.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
