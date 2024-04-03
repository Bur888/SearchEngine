package searchengine.model.searchLinks;

import lombok.Getter;
import lombok.Setter;
import org.apache.catalina.core.ApplicationContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import searchengine.model.entityes.SiteEntity;
import searchengine.services.PageCRUDService;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseWeb {
    @Autowired
    private PageCRUDService pageCRUDService;

    @Getter
    @Setter
    private volatile String url;

    @Getter
    @Setter
    private SiteEntity siteEntity;
    public static final Logger logger = LogManager.getLogger(ParseWeb.class);

    public ArrayList<Link> getLinksOnUrl() {
        ArrayList<Link> links = new ArrayList<>();

        if (!isURL(url)) {
            logger.info(url + " не является ссылкой");
            return links;
        }
        try {
            Document document = Jsoup
                    .connect(url)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .timeout(10 * 1000)
                    .get();
            logger.info("По ссылке " + url + " получили HTML");
            pageCRUDService.savePageStatusOk(siteEntity, document, HttpStatus.OK.value());
            Elements elements = document.select("body").select("a");
            for (Element element : elements) {
                String newUrl = element.absUrl("href");
                // logger.info("По ссылке " + url + " найдена ссылка " + newUrl);
                if (!isLinkOnCurrentSite(newUrl) || Link.getAllLinks().contains(newUrl)) {
                    //logger.info("В ссылке " + url + " ссылка " + newUrl + " уже отработана либо не является ссылкой на этот сайт.");
                } else {
                    Link newLink = new Link(newUrl);
                    links.add(newLink);
                    logger.info("По ссылке " + url + " добавлена ссылка " + newUrl);
                    Link.getAllLinks().add(newUrl);
                }
            }
        } catch (IOException e) {
            pageCRUDService.savePageStatusError(siteEntity, HttpStatus.BAD_GATEWAY.value());
            throw new RuntimeException(e);
        }
        return links;
    }

    public boolean isURL(String url) {
        try {
            new URL(url).openStream().close();
            return true;
        } catch (Exception ex) {
            System.out.println(ex.getStackTrace());
        }
        return false;
    }

    public boolean isLinkOnCurrentSite(String newUrl) {
        Pattern pattern = Pattern.compile(url);
        Matcher matcher = pattern.matcher(newUrl);
        return matcher.find();
    }
}

