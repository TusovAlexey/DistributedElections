FROM openjdk:8-jdk-alpine

COPY ./ElectionsController/ElectionsClient/target/ElectionsClient-1.jar ElectionsClient-1.jar
ENTRYPOINT ["java", "-jar", "./ElectionsClient-1.jar"]