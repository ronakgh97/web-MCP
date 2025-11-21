# Build Stage
FROM maven:3.9-eclipse-temurin-24 AS build
LABEL authors="ronakgh97"
WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Download dependencies
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
# Skip tests for now
RUN ./mvnw clean package -DskipTests -B

# Runtime Stage (with Playwright Java)
FROM mcr.microsoft.com/playwright/java:v1.40.0-focal

WORKDIR /app

# Install additional dependencies for more stealth & Perl for Proxy Checking
RUN apt-get update && apt-get install -y \
    curl wget fonts-liberation fonts-dejavu-core ca-certificates \
    perl libwww-perl libparallel-forkmanager-perl && \
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y --no-install-recommends nodejs && \
    rm -rf /var/lib/apt/lists/*

# Install Playwright browsers with all dependencies
RUN npx playwright install --with-deps chromium

# Copy JAR from build stage
COPY --from=build /app/target/*SNAPSHOT.jar app.jar

# Copy Scripts and Proxy List
COPY scripts/ /app/scripts/
COPY proxies.txt /app/proxies.txt
RUN chmod +x /app/scripts/*.pl

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# Set environment variables for optimal performance
ENV JAVA_OPTS="-Xmx2g -Xms4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV PLAYWRIGHT_BROWSERS_PATH="/ms-playwright"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:3000/api/v1/health || exit 1

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 3000

# Start application with JVM optimizations
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]