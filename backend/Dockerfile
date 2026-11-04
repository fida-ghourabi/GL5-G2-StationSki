FROM eclipse-temurin:8-jre-alpine

WORKDIR /app

# Le jar est produit par "mvn package" dans target/ (version lue dans le pom)
COPY target/gestion-station-skii-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]