FROM eclipse-temurin:17-jre
WORKDIR /app
# Build the JAR first with Maven; never copy .env or source credentials.
COPY --chown=10001:10001 target/gestion-station-skii-0.0.1-SNAPSHOT.jar app.jar
USER 10001:10001
EXPOSE 8087
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
