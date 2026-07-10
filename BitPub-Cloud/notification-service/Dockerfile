# Autore: Stefano Bellan Matricola 20054330
# Immagine di base contenente l'ambiente di runtime Java 21 su Alpine Linux, scelta per la sua leggerezza
FROM eclipse-temurin:21-jre-alpine

# Imposta la directory di lavoro all'interno del container
WORKDIR /app

# Copia il pacchetto jar generato dalla build all'interno dell'immagine
COPY target/*.jar app.jar

# Definisce il comando di avvio dell'applicazione
ENTRYPOINT ["java", "-jar", "app.jar"]
