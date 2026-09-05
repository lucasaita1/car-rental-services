FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml checkstyle.xml ./
COPY user-microservice/pom.xml user-microservice/
COPY user-microservice/src user-microservice/src
RUN mvn -B -ntp -f user-microservice/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/user-microservice/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]