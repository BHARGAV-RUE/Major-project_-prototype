# Step 1: Build Java Application using pre-installed Maven
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy POM files
COPY pom.xml .
COPY cloudtransfer-core/pom.xml cloudtransfer-core/
COPY cloudtransfer-stream-engine/pom.xml cloudtransfer-stream-engine/

# Copy Stream Engine source code
COPY cloudtransfer-stream-engine/src cloudtransfer-stream-engine/src

# Build Stream Engine module, skipping test compilation entirely
RUN mvn clean package -pl cloudtransfer-stream-engine -am -Dmaven.test.skip=true

# Step 2: Minimal Runtime Container
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy built JAR from builder phase
COPY --from=builder /app/cloudtransfer-stream-engine/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]