FROM eclipse-temurin:17-jre-alpine

RUN addgroup -g 10001 -S app && adduser -u 10001 -S app -G app

WORKDIR /app
COPY target/gestion-station-skii-*.jar app.jar

USER 10001
EXPOSE 8087

ENTRYPOINT ["java", "-jar", "app.jar"]