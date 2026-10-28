# Step 1: Build Java Application using pre-installed Maven
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy POM files
COPY pom.xml .
COPY cloudtransfer-core/pom.xml cloudtransfer-core/
COPY cloudtransfer-stream-engine/pom.xml cloudtransfer-stream-engine/

# Copy Core source code
COPY cloudtransfer-core/src cloudtransfer-core/src

# Build Core module
RUN mvn clean package -pl cloudtransfer-core -am -Dmaven.test.skip=true

# Remove non-executable plain jar to prevent copy collision
RUN rm -f /app/cloudtransfer-core/target/*-plain.jar

# Step 2: Minimal Runtime Container
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the executable JAR
COPY --from=builder /app/cloudtransfer-core/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]