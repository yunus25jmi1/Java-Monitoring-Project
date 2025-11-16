# Multi-stage build for optimized container size
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy pom.xml first for better layer caching
COPY pom.xml .
COPY check-style.xml .

# Download dependencies (cached if pom.xml hasn't changed)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests and checkstyle for container builds)
RUN mvn package -Dmaven.test.skip=true -Dcheckstyle.skip=true -B

# Production stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Create a non-root user for security
RUN adduser -D -u 1000 appuser && chown -R appuser:appuser /app
USER appuser

# Expose port 8080 (Cloud Run uses PORT env variable)
EXPOSE 8080

# Use environment variable PORT with fallback to 8080
ENV PORT=8080

# Run the application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
