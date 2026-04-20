# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copy POM first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# Copy source and build (skip tests — run separately in CI)
COPY src ./src
RUN mvn clean package -DskipTests -B --no-transfer-progress

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# Non-root user for security
RUN addgroup -S routineflow && adduser -S routineflow -G routineflow

COPY --from=build /app/target/*.jar app.jar

USER routineflow

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
