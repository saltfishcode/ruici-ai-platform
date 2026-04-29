ARG BUILDER_IMAGE=maven:3.9.11-eclipse-temurin-21
ARG RUNTIME_IMAGE=eclipse-temurin:21-jre

FROM ${BUILDER_IMAGE} AS builder
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package

FROM ${RUNTIME_IMAGE}
WORKDIR /app

COPY --from=builder /workspace/target/ruici-ai-platform-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
