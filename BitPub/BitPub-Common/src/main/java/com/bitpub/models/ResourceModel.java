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

    public String getLinkHref(String rel) {
        if (_links != null && _links.containsKey(rel)) {
            return _links.get(rel).getHref();
        }
        return null;
    }

    // Sottoclasse interna per mappare la struttura del link JSON
    public static class Link {
        private String href;
        public String getHref() { return href; }
    }
}