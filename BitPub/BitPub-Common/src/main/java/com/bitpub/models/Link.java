package com.bitpub.models;

import com.google.gson.annotations.Expose;

/**
 * Modello DTO base per mappare i link HATEOAS provenienti dal server.
 */
public class Link {

    @Expose
    private String href;

    public Link() {
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }
}