FROM maven:3.9.1-eclipse-temurin-17
WORKDIR /project

COPY pom.xml /project/pom.xml
COPY analyzer/pom.xml /project/analyzer/pom.xml
RUN mvn dependency:copy-dependencies

COPY analyzer/src /project/analyzer/src
# Make sure to have the local config in the current directory
COPY config.yml /project/config.yml
RUN mvn package

ENTRYPOINT ["java", "-jar", "/project/analyzer/target/analyzer-1.0-SNAPSHOT.jar"]