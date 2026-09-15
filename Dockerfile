# Giai doan 1: build project bang Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Giai doan 2: chay ung dung voi JRE nhe
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/smart-canteen-jar-with-dependencies.jar app.jar
COPY public ./public
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
