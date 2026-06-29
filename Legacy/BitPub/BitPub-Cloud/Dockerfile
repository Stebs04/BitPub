# Stage 1: Build con Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copia i pom.xml (inclusi i moduli dichiarati nel root pom)
COPY pom.xml ./
COPY BitPub-Common/pom.xml BitPub-Common/
COPY BitPub-Edge/pom.xml BitPub-Edge/
COPY BitPub-Cloud/pom.xml BitPub-Cloud/
COPY BitPub-JavaFX/pom.xml BitPub-JavaFX/
COPY BitPub-Simulators/pom.xml BitPub-Simulators/

# Copia solo i sorgenti dei moduli necessari al backend
COPY BitPub-Common/src BitPub-Common/src
COPY BitPub-Edge/src BitPub-Edge/src
COPY BitPub-Cloud/src BitPub-Cloud/src

# Build solo di BitPub-Cloud e le sue dipendenze dirette
RUN mvn clean package -DskipTests -pl BitPub-Cloud -am

# Stage 2: Runtime minimale
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/BitPub-Cloud/target/bitpub-cloud-1.0-SNAPSHOT-exec.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]