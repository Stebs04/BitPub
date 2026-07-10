# Autore: Luca Franzon 20054744
#
# Configurazione per la creazione dell'immagine Docker del servizio utenti.
# Definisce l'ambiente di esecuzione basato su Java 21 e le istruzioni 
# per eseguire correttamente l'applicazione all'interno di un container.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
