# Étape 1: Build avec Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copier le fichier pom.xml pour télécharger les dépendances (cache Docker)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copier le code source et builder
COPY src ./src
RUN mvn clean package -DskipTests

# Étape 2: Runtime avec JRE seulement
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copier le JAR depuis l'étape de build
COPY --from=build /app/target/payment-service-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]