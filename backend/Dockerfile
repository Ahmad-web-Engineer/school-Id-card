# Stage 1: Build the backend application
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace/app

# Install maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy pom and source
COPY pom.xml .
COPY src ./src

# Build production jar
RUN mvn clean package -DskipTests

# Stage 2: Production runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install fontconfig and fonts for Apache PDFBox headless card rendering
RUN apt-get update && apt-get install -y --no-install-recommends \
    fontconfig \
    fonts-dejavu-core \
    curl \
 && rm -rf /var/lib/apt/lists/*

# Run as non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser -d /app -s /sbin/nologin appuser

# Create upload directory and grant permissions
RUN mkdir -p /app/uploads && chown -R appuser:appuser /app

USER appuser

COPY --from=builder --chown=appuser:appuser /workspace/app/target/school-id-card-platform-*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
ENV APP_STORAGE_LOCATION=/app/uploads

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/health || exit 1

ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "app.jar"]
