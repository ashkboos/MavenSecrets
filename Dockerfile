FROM maven:3.9.1-eclipse-temurin-17 AS build
WORKDIR /project

COPY pom.xml /project/pom.xml
COPY analyzer/pom.xml /project/analyzer/pom.xml
RUN mvn dependency:copy-dependencies

COPY analyzer/src /project/analyzer/src
# Make sure to have the local config in the current directory
COPY config.yml /project/config.yml
RUN mvn package

FROM eclipse-temurin:17-jre-alpine AS runtime
COPY --from=build /project/analyzer/target/analyzer-1.0-SNAPSHOT.jar /project/analyzer.jar
COPY --from=build /project/analyzer/src/main/resources/config.yml /project/config.yml

WORKDIR /project
ENTRYPOINT ["java", "-jar", "/project/analyzer.jar"]