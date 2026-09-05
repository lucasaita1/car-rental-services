FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml checkstyle.xml ./
COPY email-microservice/pom.xml email-microservice/
COPY email-microservice/src email-microservice/src
RUN mvn -B -ntp -f email-microservice/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/email-microservice/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]