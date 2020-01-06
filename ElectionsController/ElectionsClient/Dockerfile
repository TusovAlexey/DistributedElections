FROM openjdk:8-jdk-alpine

COPY . /elections
WORKDIR /elections/ElectionsController/ElectionsClient
ENTRYPOINT ["java", "-jar", "./target/ElectionsClient-1.jar"]