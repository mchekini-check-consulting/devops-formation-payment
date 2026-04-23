# # Étape 1: Build avec Maven
# FROM maven:3.9.6-eclipse-temurin-17 AS build

# WORKDIR /app

# # Copier le fichier pom.xml pour télécharger les dépendances (cache Docker)
# COPY pom.xml .
# RUN mvn dependency:go-offline

# # Copier le code source et builder
# COPY src ./src
# RUN mvn clean package -DskipTests

# # Étape 2: Runtime avec JRE seulement
# FROM eclipse-temurin:17-jdk-alpine

# WORKDIR /app

# # Copier le JAR depuis l'étape de build
# COPY --from=build /app/target/payment-service-*.jar app.jar

# EXPOSE 8082

# ENTRYPOINT ["java", "-jar", "app.jar"]
# Étape 1: Build du JAR avec Maven

# FROM maven:3.9.6-eclipse-temurin-17 as maven-builder

# WORKDIR /app
# COPY pom.xml .
# COPY src ./src
# RUN mvn clean package -DskipTests

# # Étape 2: Compilation native avec GraalVM (version Ubuntu corrigée)
# FROM ubuntu:22.04 as graalvm-builder

# # Installer les dépendances système
# RUN apt-get update && \
#     apt-get install -y wget tar gcc libz-dev libstdc++-12-dev && \
#     rm -rf /var/lib/apt/lists/*

# # Installer GraalVM
# WORKDIR /opt
# RUN wget https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-21.0.2/graalvm-community-jdk-21.0.2_linux-x64_bin.tar.gz && \
#     tar -xzf graalvm-community-jdk-21.0.2_linux-x64_bin.tar.gz && \
#     rm graalvm-community-jdk-21.0.2_linux-x64_bin.tar.gz

# # Définir JAVA_HOME (native-image est déjà inclus dans cette version)
# ENV JAVA_HOME=/opt/graalvm-community-openjdk-21.0.2+13.1
# ENV PATH=$JAVA_HOME/bin:$PATH

# # Vérifier que native-image est disponible
# RUN ls -la $JAVA_HOME/bin/ | grep native-image

# WORKDIR /app
# COPY --from=maven-builder /app/target/*.jar app.jar

# # Compiler en natif (native-image est déjà dans le PATH)
# RUN native-image -jar app.jar \
#     --no-fallback \
#     -H:+ReportExceptionStackTraces \
#     -H:+AddAllCharsets \
#     -H:IncludeResources="application.*" \
#     -H:Name=application

# # Étape 3: Image finale
# FROM alpine:latest

# RUN apk add --no-cache libstdc++ libgcc ca-certificates \
#     && addgroup -S spring && adduser -S spring -G spring

# COPY --from=graalvm-builder /app/application /app/application
# RUN chown -R spring:spring /app

# USER spring:spring
# EXPOSE 8082
# ENTRYPOINT ["/app/application"]


# # ─── Stage 1 : Build natif avec GraalVM + Maven ───────────────────────────────
# FROM ubuntu:22.04 AS native-builder

# RUN apt-get update && \
#     apt-get install -y wget tar gcc libz-dev libstdc++-12-dev && \
#     rm -rf /var/lib/apt/lists/*

# WORKDIR /opt

# RUN wget -q https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-21.0.2/graalvm-community-jdk-21.0.2_linux-x64_bin.tar.gz && \
#     tar -xzf graalvm-community-jdk-21.0.2_linux-x64_bin.tar.gz && \
#     mv graalvm-community-openjdk-21.0.2+13.1 graalvm-21 && \
#     rm graalvm-community-jdk-21.0.2_linux-x64_bin.tar.gz

# ENV JAVA_HOME=/opt/graalvm-21
# ENV PATH=$JAVA_HOME/bin:$PATH

# RUN wget -q https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz && \
#     tar -xzf apache-maven-3.9.6-bin.tar.gz && \
#     rm apache-maven-3.9.6-bin.tar.gz

# ENV MAVEN_HOME=/opt/apache-maven-3.9.6
# ENV PATH=$MAVEN_HOME/bin:$PATH

# RUN java -version && mvn --version && native-image --version

# WORKDIR /app
# COPY pom.xml .
# RUN mvn dependency:go-offline -q

# COPY src ./src

# RUN mvn -Pnative -DskipTests package

# RUN ls -la /app/target/payment-service

# # ─── Stage 2 : Image finale minimale ─────────────────────────────────────────
# FROM debian:bookworm-slim

# RUN apt-get update && \
#     apt-get install -y --no-install-recommends \
#         ca-certificates \
#         libstdc++6 \
#         libgcc-s1 \
#     && rm -rf /var/lib/apt/lists/* \
#     && addgroup --system spring \
#     && adduser --system --group spring

# COPY --from=native-builder /app/target/payment-service /app/payment-service
# RUN chmod +x /app/payment-service

# USER spring:spring
# EXPOSE 8082
# ENTRYPOINT ["/app/payment-service"]





# ─── Stage 1 : Build JAR ──────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

RUN mvn package -DskipTests

# ─── Stage 2 : Image finale minimale ─────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app
COPY --from=builder /app/target/payment-service-0.0.1-SNAPSHOT.jar app.jar

USER spring:spring
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]





























