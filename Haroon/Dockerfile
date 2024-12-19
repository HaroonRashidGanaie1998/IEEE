# Stage 1: Build Stage
# Use an official Maven image with OpenJDK 17
FROM maven:3.9.4-eclipse-temurin-17 as build

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven configuration and dependencies for caching
COPY pom.xml /app/pom.xml

# Download Maven dependencies (will cache this layer if pom.xml is unchanged)
RUN mvn dependency:go-offline -B

# Copy the entire source code
COPY src /app/src

# Build the Spring Boot application using Maven
RUN mvn clean package -DskipTests

# Stage 2: Runtime Stage
# Use a lightweight OpenJDK 17 runtime image
FROM eclipse-temurin:17-jdk-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the built JAR file from the build stage
COPY --from=build /app/target/Haroon-1.0-SNAPSHOT.jar /app/Haroon-1.0-SNAPSHOT.jar

# Expose port 8090 for the application
EXPOSE 8090

# Command to run the Spring Boot application
ENTRYPOINT ["java", "-jar", "/app/Haroon-1.0-SNAPSHOT.jar"]
