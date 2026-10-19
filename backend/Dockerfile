FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -B clean package
FROM eclipse-temurin:17-jre
RUN useradd --system --create-home app && mkdir /app && chown app:app /app
USER app
WORKDIR /app
COPY --from=build /build/target/kaamfolio-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
