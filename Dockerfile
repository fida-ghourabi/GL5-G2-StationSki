FROM eclipse-temurin:8-jre-alpine

WORKDIR /app

# Le jar est produit par "mvn package" dans target/
COPY target/gestion-station-skii-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]