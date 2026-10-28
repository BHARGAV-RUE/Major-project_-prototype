# Step 1: Build Java Application using JDK 21
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy root Maven wrapper and POM files for efficient caching
COPY mvnw .
COPY pom.xml .
COPY cloudtransfer-core/pom.xml cloudtransfer-core/
COPY cloudtransfer-stream-engine/pom.xml cloudtransfer-stream-engine/

# Copy Core source code
COPY cloudtransfer-core/src cloudtransfer-core/src

# Build only the core module
RUN ./mvnw clean package -pl cloudtransfer-core -am -DskipTests

# Step 2: Minimal Runtime Container
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy built JAR from builder phase
COPY --from=builder /app/cloudtransfer-core/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]