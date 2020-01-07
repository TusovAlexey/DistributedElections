FROM openjdk:8-jdk

COPY . /elections
WORKDIR /elections/ElectionsController/ElectionsServer
ENTRYPOINT ["java", "-jar", "./target/ElectionsServer-1.jar"]
#ENTRYPOINT ["java", "-Xdebug", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005", "-Djava.security.egd=file:/dev/./urandom","-jar","./target/ElectionsServer-1.jar"]

