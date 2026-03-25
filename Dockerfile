# Shared multi-stage build for every service: docker build --build-arg SERVICE=event-service .
FROM maven:3.9-eclipse-temurin-21 AS build
ARG SERVICE
WORKDIR /src

# Resolve dependencies first so code edits don't bust the dependency layer.
COPY pom.xml .
COPY common/pom.xml common/
COPY auth-service/pom.xml auth-service/
COPY event-service/pom.xml event-service/
COPY ticket-service/pom.xml ticket-service/
COPY payment-service/pom.xml payment-service/
COPY notification-service/pom.xml notification-service/
RUN mvn -B -q -pl ${SERVICE} -am dependency:go-offline

COPY common common
COPY ${SERVICE} ${SERVICE}
RUN mvn -B -q -pl ${SERVICE} -am package -DskipTests \
    && cp ${SERVICE}/target/app.jar /app.jar

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /app.jar app.jar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
    CMD wget -qO- http://localhost:${PORT:-8080}/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
