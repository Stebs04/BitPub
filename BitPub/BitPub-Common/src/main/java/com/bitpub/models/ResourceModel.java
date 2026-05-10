package com.bitpub.models;

import java.util.Map;

/**
 * Classe base per supportare HATEOAS nel client.
 * Ogni oggetto ricevuto dal server che estende questa classe avrà i suoi link.
 */
public class ResourceModel {
    // Il campo _links è lo standard HATEOAS implementato da Spring nel backend
    private Map<String, Link> _links;

    public Map<String, Link> getLinks() {
        return _links;
    }

    public void setLinks(Map<String, Link> _links) {
        this._links = _links;
    }

    public void addLink(String rel, String href) {
        if (this._links == null) {
            this._links = new java.util.HashMap<>();
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
        private String href;
        public Link() {}
        public Link(String href) { this.href = href; }
        public String getHref() { return href; }
    }
}