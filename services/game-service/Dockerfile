# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

COPY pom.xml .
COPY bitpub-common/pom.xml bitpub-common/
COPY shared-contracts/pom.xml shared-contracts/
COPY shared-domain/pom.xml shared-domain/
COPY shared-persistence/pom.xml shared-persistence/
COPY auth-contracts/pom.xml auth-contracts/
COPY shared-mqtt/pom.xml shared-mqtt/
COPY infra/discovery-server/pom.xml infra/discovery-server/
COPY infra/config-server/pom.xml infra/config-server/
COPY infra/api-gateway/pom.xml infra/api-gateway/
COPY services/auth-service/pom.xml services/auth-service/
COPY services/game-service/pom.xml services/game-service/
COPY services/tournament-service/pom.xml services/tournament-service/
COPY services/stats-service/pom.xml services/stats-service/
COPY services/edge-sync-service/pom.xml services/edge-sync-service/

COPY bitpub-common/src bitpub-common/src
COPY shared-contracts/src shared-contracts/src
COPY shared-domain/src shared-domain/src
COPY shared-persistence/src shared-persistence/src
COPY auth-contracts/src auth-contracts/src
COPY shared-mqtt/src shared-mqtt/src
COPY services/game-service/src services/game-service/src

RUN mvn clean package -pl services/game-service -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S bitpub && adduser -S bitpub -G bitpub
USER bitpub

COPY --from=builder /app/services/game-service/target/*.jar app.jar

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
