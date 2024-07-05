package searchengine.model.searchLinks;

import lombok.Getter;
import lombok.Setter;
import searchengine.dto.entityesToDto.PageToDto;

import java.util.ArrayList;
import java.util.HashSet;

@Setter
@Getter
public class Link {
    private String url;
    private String root;
    private final int siteId;
    private static HashSet<String> allLinks = new HashSet<>();
    private ArrayList<Link> links;
    @Setter
    @Getter
    private static HashSet<String> rootUrls = new HashSet<>();

    public Link(String link, int siteId, String root) {
        this.url = link;
        this.siteId = siteId;
        this.root = root;
        links = new ArrayList<>();
    }

    public synchronized static HashSet<String> getAllLinks() {
        return allLinks;
    }

    public synchronized static void setAllLinks(HashSet<String> allLinks) {
        Link.allLinks = allLinks;
    }

    public static void removeAllLinks(ArrayList<PageToDto> list) {
        for (PageToDto page : list) {
            allLinks.remove(page.getPath());
        }
    }

    public static String urlWithoutRoot(String url) {
        for (String root : rootUrls) {
            if (url.startsWith(root)) {
                return url.replaceAll(root, "");
            }
        }
        return null;
    }

    public static String clearUrl(String url) {
        String updateUrl = url.replaceAll("#", "");
        return updateUrl.endsWith("/") ? updateUrl.substring(0, updateUrl.length() - 1) : updateUrl;
    }
}