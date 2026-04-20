package com.bitpub.controllers;

import com.bitpub.models.BiliardoResource;
import com.google.gson.Gson;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseBody;

@RestController
public class BiliardoController {

    private final Gson gson = new Gson();

    @GetMapping("/api/v1/biliardo/eventi/{id}")
    public @ResponseBody String getEventoBiliardo(@PathVariable String id) {
        // Supponiamo di recuperare i dati dal DB PostgreSQL [cite: 41, 51]
        // Luca, ricorda che se accedi a dati condivisi devi usare 'synchronized' [cite: 52, 75]
        BiliardoResource evento = new BiliardoResource(id, "PALLA_IMBUCATA", "Team_Luca", "Match_001");

        // GSON genera la struttura annidata correttamente prima dell'invio
        return gson.toJson(evento);
    }
}