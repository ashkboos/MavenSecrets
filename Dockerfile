FROM maven:3.9.1-eclipse-temurin-17
WORKDIR /project

COPY pom.xml /project/pom.xml
COPY analyzer/pom.xml /project/analyzer/pom.xml
RUN mvn dependency:copy-dependencies

COPY analyzer/src /project/analyzer/src
RUN mvn package

ENTRYPOINT ["java", "-jar", "/project/analyzer/target/analyzer-1.0-SNAPSHOT.jar"]