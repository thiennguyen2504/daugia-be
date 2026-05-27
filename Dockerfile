FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw mvnw
COPY pom.xml pom.xml
RUN chmod +x mvnw; ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache tzdata docker-cli
ENV TZ=Asia/Ho_Chi_Minh

RUN mkdir -p /app/logs /backups

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
