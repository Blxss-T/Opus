# --- Build stage: compiles the application jar (tests run in CI, not here) ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Resolve dependencies first so they are cached until pom.xml changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# --- Runtime stage: JRE only, non-root ---
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --no-create-home --shell /usr/sbin/nologin opus
COPY --from=build /build/target/opsflow-*.jar app.jar

USER opus
EXPOSE 8080

# Container-aware heap sizing; override with JAVA_OPTS.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
