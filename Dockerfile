FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml /app/pom.xml
COPY src /app/src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/ctg-user-service-*.jar /app/ctg-user-service.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/ctg-user-service.jar"]