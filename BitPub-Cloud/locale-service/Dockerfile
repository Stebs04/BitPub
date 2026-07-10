# Autore: Stefano Bellan Matricola 20054330
# Immagine di base leggera con JRE 21 per eseguire l'applicazione Spring Boot
FROM eclipse-temurin:21-jre-alpine

# Imposto la directory di lavoro all'interno del container
WORKDIR /app

# Copio il jar compilato dalla cartella target nell'immagine
COPY target/*.jar app.jar

# Definisco il comando di avvio del microservizio
ENTRYPOINT ["java", "-jar", "app.jar"]
