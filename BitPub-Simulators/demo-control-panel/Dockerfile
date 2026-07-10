# autore Timothy Giolito 20054431
# Definizione dell'immagine base e delle istruzioni di build per il servizio di controllo pannello demo.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
