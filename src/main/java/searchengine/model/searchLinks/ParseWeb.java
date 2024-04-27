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
        String urlClear = Link.clearUrl(link.getUrl());
        String urlWithoutRoot = new String();

        if (!isURL(urlClear)) {
            logger.info(urlClear + " не является ссылкой");
            return links;
        }
        try {
            if (!isUrlInLinksAndDB(urlClear, link.getSiteId())) {
                Document document = connection.getDocument(link.getUrl());
                urlWithoutRoot = Link.urlWithoutRoot(urlClear);
                //if (updateUrl != null) {
                    pageToDto = pageToDto.makePageToDtoForSave(link.getSiteId(), urlWithoutRoot, String.valueOf(document), HttpStatus.OK.value());
                //}
                Elements elements = document.select("a[href]");
                for (Element element : elements) {
                    String newUrl = Link.clearUrl(element.attr("abs:href"));
                    if (isLinkOnCurrentSite(newUrl) & !isUrlInLinksAndDB(newUrl, link.getSiteId())) {
                        Link newLink = new Link(newUrl, link.getSiteId(), link.getRoot());
                        links.add(newLink);
                        logger.info("По ссылке " + urlClear + " добавлена ссылка " + newUrl);
/*
                Elements elements = document.select("body").select("a");
                for (Element element : elements) {
                    String newUrl = Link.clearUrl(element.absUrl("href"));
                    if (isLinkOnCurrentSite(newUrl) & !isUrlInLinksAndDB(newUrl, link.getSiteId())) {
                        Link newLink = new Link(newUrl, link.getSiteId());
                        links.add(newLink);
                        logger.info("По ссылке " + link.getUrl() + " добавлена ссылка " + newUrl);
*/
                    }
                }
            }
        } catch (HttpStatusException ex) {
            if (pageToDto.getPath() != null && !ParseWebRecursive.isStopNow()) {
                pageToDto = pageToDto.makePageToDtoForSave(link.getSiteId(), urlWithoutRoot, null, ex.getStatusCode());
                Link.getAllLinks().add(urlClear);
            }
        } catch (IOException e) {
            if (pageToDto.getPath() != null && !ParseWebRecursive.isStopNow()) {
                pageToDto = pageToDto.makePageToDtoForSave(link.getSiteId(), urlWithoutRoot, null, HttpStatus.BAD_GATEWAY.value());
                Link.getAllLinks().add(urlClear);
            }
        } finally {
            if (pageToDto.getPath() != null && !ParseWebRecursive.isStopNow()) {
                PageToDto.getPageToDtoList().add(pageToDto);
                Link.getAllLinks().add(urlClear);
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
    public boolean isLinkOnCurrentSite(String newUrl) {
        Pattern pattern = Pattern.compile(link.getRoot());
        Matcher matcher = pattern.matcher(newUrl);
        return matcher.find();
    }

    public boolean isUrlInLinksAndDB(String url, Integer siteId) {
        return Link.getAllLinks().contains(url) || pageCRUDService.isUrlInDB(Link.urlWithoutRoot(url), siteId);
    }

}
