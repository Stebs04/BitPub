# Autore: Luca Franzon 20054744
# Questo Dockerfile definisce l'immagine di base e le direttive necessarie 
# per la creazione e l'esecuzione del container del servizio di autenticazione.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
