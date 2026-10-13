FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B verify
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system chaser && useradd --system --gid chaser chaser && chown chaser:chaser /app
COPY --from=build /src/target/finance-service-0.1.0.jar app.jar
USER chaser
ENV PORT=8080 BIND_ADDRESS=0.0.0.0 APP_MODE=cloud
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
