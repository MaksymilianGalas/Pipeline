FROM openjdk:21-jdk AS build

USER root

COPY . .

RUN chmod +x ./mvnw

RUN ./mvnw clean install -Pproduction

FROM eclipse-temurin:21-jre
COPY --from=build  ./target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
