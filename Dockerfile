FROM eclipse-temurin:24-jdk-alpine

WORKDIR /app

# Add a non-root user to run the application
RUN addgroup -S spring && adduser -S spring -G spring

# Create directory for file uploads and set permissions (as root)
RUN mkdir -p /app/uploads && chown -R spring:spring /app/uploads && chmod 755 /app/uploads

# Copy JAR file
COPY --chown=spring:spring target/greenbook-0.0.1-SNAPSHOT.jar /app/app.jar

# Switch to non-root user
USER spring:spring

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]