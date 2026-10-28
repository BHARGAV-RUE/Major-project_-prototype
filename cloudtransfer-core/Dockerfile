# Step 1: Build Java Application using pre-installed Maven
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy POM files
COPY pom.xml .
COPY cloudtransfer-core/pom.xml cloudtransfer-core/
COPY cloudtransfer-stream-engine/pom.xml cloudtransfer-stream-engine/

# Copy Core source code
COPY cloudtransfer-core/src cloudtransfer-core/src

# Build Core module, skipping test compilation entirely
RUN mvn clean package -pl cloudtransfer-core -am -Dmaven.test.skip=true

# Step 2: Minimal Runtime Container
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy built JAR from builder phase
COPY --from=builder /app/cloudtransfer-core/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]