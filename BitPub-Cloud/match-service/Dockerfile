# Autore: Timothy Giolito 20054431
# Usa l'immagine base per Java 21, basata su Alpine per ridurre l'impronta di memoria
FROM eclipse-temurin:21-jre-alpine
# Imposta la directory operativa all'interno del container
WORKDIR /app
# Trasferisce l'eseguibile compilato all'interno del container
COPY target/*.jar app.jar
# Comando di avvio del servizio
ENTRYPOINT ["java", "-jar", "app.jar"]
