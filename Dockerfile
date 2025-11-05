# -------------------------
# Dockerfile - Spring Boot app
# -------------------------
# -----------------------------
# Étape 1 : Build avec Maven
# -----------------------------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests


# -----------------------------
# Étape 2 : Exécution de l'app
# -----------------------------
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=build /app/target/adhesion-app-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java","-jar","app.jar"]
