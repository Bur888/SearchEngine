package searchengine.model.searchLinks;

import java.util.ArrayList;
import java.util.HashSet;

public class Link {
    private String url;
    private final int siteId;
    private static HashSet<String> allLinks = new HashSet<>();
    private ArrayList<Link> links;

    public Link(String link, int siteId) {
        this.url = link;
        this.siteId = siteId;
        links = new ArrayList<>();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getSiteId() {
        return siteId;
    }

    public ArrayList<Link> getLinks() {
        return links;
    }

    public void setLinks(ArrayList<Link> links) {
        this.links = links;
    }

    public synchronized static HashSet<String> getAllLinks() {
        return allLinks;
    }

    public synchronized static void setAllLinks(HashSet<String> allLinks) {
        Link.allLinks = allLinks;
    }
}
