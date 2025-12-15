FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl

WORKDIR /app
COPY matching-engine/target/matching-engine*.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]