FROM openjdk:8-jdk-alpine

COPY . /elections
WORKDIR /elections/ElectionsController/ElectionsServer
ENTRYPOINT ["java", "-jar", "./target/ElectionsServer-1.jar"]
