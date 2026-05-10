package com.bitpub.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.HashMap;

/**
 * Classe base per supportare HATEOAS nel client.
 * Ogni oggetto ricevuto dal server che estende questa classe avrà i suoi link.
 */
public class ResourceModel {
    
    // Il campo _links è lo standard HATEOAS implementato da Spring nel backend
    @Expose
    @SerializedName("_links")
    @JsonProperty("_links")
    private Map<String, Link> _links;

    @JsonProperty("_links")
    public Map<String, Link> getLinks() {
        if (_links == null) {
            _links = new HashMap<>();
        }
        return _links;
    }

    @JsonProperty("_links")
    public void setLinks(Map<String, Link> _links) {
        this._links = _links;
    }

    public void addLink(String rel, String href) {
        if (this._links == null) {
            this._links = new HashMap<>();
        }
        this._links.put(rel, new Link(href));
    }

    public String getLinkHref(String rel) {
        if (_links != null && _links.containsKey(rel)) {
            return _links.get(rel).getHref();
        }
        return null;
    }

    // Sottoclasse interna per mappare la struttura del link JSON
    public static class Link {
        @Expose
        @JsonProperty("href")
        private String href;
        
        public Link() {}
        public Link(String href) { this.href = href; }
        
        @JsonProperty("href")
        public String getHref() { return href; }
        
        @JsonProperty("href")
        public void setHref(String href) { this.href = href; }
    }
}
