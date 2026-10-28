# Step 1: Build Java Application using JDK 21
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy root Maven wrapper and POM files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY cloudtransfer-core/pom.xml cloudtransfer-core/
COPY cloudtransfer-stream-engine/pom.xml cloudtransfer-stream-engine/

# Copy Stream Engine source code
COPY cloudtransfer-stream-engine/src cloudtransfer-stream-engine/src

# Build only the stream module
RUN ./mvnw clean package -pl cloudtransfer-stream-engine -am -DskipTests

# Step 2: Minimal Runtime Container
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy built JAR from builder phase
COPY --from=builder /app/cloudtransfer-stream-engine/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]