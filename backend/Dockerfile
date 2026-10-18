FROM eclipse-temurin:8-jre

WORKDIR /app
COPY target/gestion-station-skii-0.0.1-SNAPSHOT.jar /app/app.jar

USER 10001:10001
EXPOSE 8087
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
