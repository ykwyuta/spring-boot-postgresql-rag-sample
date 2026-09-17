FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B verify

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/target/transport-rag-0.0.1-SNAPSHOT.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
