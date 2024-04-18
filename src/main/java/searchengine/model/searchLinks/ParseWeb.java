package searchengine.model.searchLinks;

import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import searchengine.dto.entityesToDto.PageToDto;
import searchengine.services.PageCRUDService;
import searchengine.services.SiteCRUDService;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseWeb {

    private SiteCRUDService siteCRUDService;
    private PageCRUDService pageCRUDService;
    private ConnectionWeb connection = new ConnectionWeb();

    @Getter
    @Setter
    private Link link;

    public ParseWeb(SiteCRUDService siteCRUDService, PageCRUDService pageCRUDService) {
        this.siteCRUDService = siteCRUDService;
        this.pageCRUDService = pageCRUDService;
    }

    public static final Logger logger = LogManager.getLogger(ParseWeb.class);

    public ArrayList<Link> getLinksOnUrl() {
        ArrayList<Link> links = new ArrayList<>();
        PageToDto pageToDto = new PageToDto();

        if (!isURL(link.getUrl())) {
            logger.info(link.getUrl() + " не является ссылкой");
            return links;
        }
        try {
            // Document document = connection.getDocument(link.getUrl());
            if (!isUrlInLinksAndDB(link.getUrl(), link.getSiteId())) {
                Document document = connection.getDocument(link.getUrl());
                Link.getAllLinks().add(link.getUrl());
                pageToDto = pageToDto.makePageToDtoForSave(link, String.valueOf(document), HttpStatus.OK.value());

                Elements elements = document.select("body").select("a");
                for (Element element : elements) {
                    String newUrl = element.absUrl("href");
                    if (isLinkOnCurrentSite(newUrl) & !isUrlInLinksAndDB(newUrl, link.getSiteId())) {
                        Link newLink = new Link(newUrl, link.getSiteId());
                        links.add(newLink);
                        logger.info("По ссылке " + link.getUrl() + " добавлена ссылка " + newUrl);
                    }
                }
            }
        } catch (HttpStatusException ex) {
            pageToDto = pageToDto.makePageToDtoForSave(link, null, ex.getStatusCode());
            Link.getAllLinks().add(link.getUrl());
        } catch (IOException e) {
            pageToDto = pageToDto.makePageToDtoForSave(link, null, HttpStatus.BAD_GATEWAY.value());
            Link.getAllLinks().add(link.getUrl());
        } finally {
            if (pageToDto.getPath() != null) {
                PageToDto.getPageToDtoList().add(pageToDto);
            }
        }
        return links;
    }

    public boolean isURL(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception ex) {
            System.out.println(ex.getStackTrace());
        }
        return false;
    }
/*
    public boolean isURL(String url) {
        try {
            new URL(url).openStream().close();
            return true;
        } catch (Exception ex) {
            System.out.println(ex.getStackTrace());
        }
        return false;
    }
*/
    public boolean isLinkOnCurrentSite(String newUrl) {
        Pattern pattern = Pattern.compile(link.getUrl());
        Matcher matcher = pattern.matcher(newUrl);
        return matcher.find();
    }

    public boolean isUrlInLinksAndDB(String url, Integer siteId) {
        return Link.getAllLinks().contains(url) || pageCRUDService.isUrlInDB(url, siteId);
    }
}