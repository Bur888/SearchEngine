package searchengine.model.searchLinks;

import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import searchengine.dto.entityesToDto.PageToDto;
import searchengine.model.entityes.SiteEntity;
import searchengine.model.entityes.StatusIndexing;
import searchengine.services.PageCRUDService;
import searchengine.services.SiteCRUDService;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseWeb {

    private SiteCRUDService siteCRUDService;
    private PageCRUDService pageCRUDService;

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
        SiteEntity siteEntity = siteCRUDService.getById(link.getSiteId());

        if (!isURL(link.getUrl())) {
            logger.info(link.getUrl() + " не является ссылкой");
            return links;
        }
        try {
            Document document = Jsoup
                    .connect(link.getUrl())
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .timeout(10 * 1000)
                    .get();

            PageToDto pageToDto = makePageToDtoForSave(
                    link.getSiteId(), link.getUrl(), String.valueOf(document), HttpStatus.OK.value()
            );
            pageCRUDService.savePage(pageToDto);
            siteEntity.setStatusTime(LocalDateTime.now());
            siteCRUDService.save(siteEntity);

            Elements elements = document.select("body").select("a");
            for (Element element : elements) {
                String newUrl = element.absUrl("href");
                if (!isLinkOnCurrentSite(newUrl) || !pageCRUDService.isUrlLInDB(newUrl, link.getSiteId())) {
                } else {
                    Link newLink = new Link(newUrl, link.getSiteId());
                    links.add(newLink);
                    logger.info("По ссылке " + link.getUrl() + " добавлена ссылка " + newUrl);
                    //Link.getAllLinks().add(newUrl);
                }
            }
        } catch (IOException e) {
            PageToDto pageToDto = makePageToDtoForSave(
                    link.getSiteId(), link.getUrl(), null, HttpStatus.BAD_GATEWAY.value()
            );
            pageCRUDService.savePage(pageToDto);
            siteEntity.setLastError("Ошибка при чтении ссылки " + siteEntity.getUrl());
            siteEntity.setStatusIndexing(StatusIndexing.FAILED);
            siteCRUDService.save(siteEntity);
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
        Pattern pattern = Pattern.compile(link.getUrl());
        Matcher matcher = pattern.matcher(newUrl);
        return matcher.find();
    }

    public boolean isLinkInDB(String url, Integer siteId) {


        return true;
    }

    public PageToDto makePageToDtoForSave(Integer siteId, String url, String document, Integer code) {
        PageToDto pageToDto = new PageToDto();
        pageToDto.setSiteId(siteId);
        pageToDto.setPath(url);
        pageToDto.setContent(String.valueOf(document));
        pageToDto.setCode(code);
        return pageToDto;
    }
}

