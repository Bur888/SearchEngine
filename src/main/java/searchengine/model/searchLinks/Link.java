package searchengine.model.searchLinks;

import java.util.ArrayList;
import java.util.HashSet;

public class Link {
    private String link;
    private int siteId;
    private static HashSet<String> allLinks = new HashSet<>();
    private ArrayList<Link> links;

    public Link(String link) {
        this.link = link;
        links = new ArrayList<>();
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
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
