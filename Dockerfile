# Report microservice (workload-service)
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN mkdir -p /app/logs
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENV SPRING_PROFILES_ACTIVE=standalone
ENV LOG_FILE=/app/logs/application.log
ENTRYPOINT ["java", "-jar", "app.jar"]
