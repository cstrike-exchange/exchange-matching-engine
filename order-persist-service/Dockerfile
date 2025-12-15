FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY ./order-persist-service/target/order-persist-service.jar /app
CMD ["java", "-jar", "order-persist-service.jar"]