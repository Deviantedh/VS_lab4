ARG MODULE=gateway-service

FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY common-dto common-dto
COPY discovery-service discovery-service
COPY config-service config-service
COPY gateway-service gateway-service
COPY employee-service employee-service
COPY attendance-service attendance-service
COPY schedule-service schedule-service

ARG MODULE
RUN chmod +x ./mvnw
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw clean package -pl ${MODULE} -am -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app


COPY config-repo /app/config-repo

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

ARG MODULE
COPY --from=builder /build/${MODULE}/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
