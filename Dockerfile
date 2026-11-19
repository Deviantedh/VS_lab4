ARG MODULE=gateway-service

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY config-repo /app/config-repo

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app/uploads
USER appuser

ARG MODULE
COPY ${MODULE}/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
