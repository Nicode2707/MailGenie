FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Build skipped for scaffolding speed in Epic 7

FROM eclipse-temurin:17-jre
WORKDIR /app
# COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
