FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml checkstyle.xml ./
COPY car-microservice/pom.xml car-microservice/
COPY car-microservice/src car-microservice/src
RUN mvn -B -ntp -f car-microservice/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/car-microservice/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]