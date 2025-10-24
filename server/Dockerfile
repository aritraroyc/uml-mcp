# Multi-stage build for UML MCP Server
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Install graphviz (required for PlantUML)
RUN apk add --no-cache graphviz ttf-dejavu

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/target/uml-mcp-server-1.0.0.jar /app/server.jar

# Expose the default port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

# Run the server
ENTRYPOINT ["java", "-jar", "/app/server.jar"]
CMD ["--host", "0.0.0.0", "--port", "8080"]
