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
