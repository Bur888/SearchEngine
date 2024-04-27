package searchengine.model.searchLinks;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.HttpStatusException;
import searchengine.config.Site;
import searchengine.dto.entityesToDto.PageToDto;
import searchengine.model.entityes.SiteEntity;
import searchengine.model.entityes.StatusIndexing;
import searchengine.services.indexingService.IndexingService;
import searchengine.services.PageCRUDService;
import searchengine.services.SiteCRUDService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.logging.Logger;

public class ThreadForSearchLinks implements Runnable {
    private SiteCRUDService siteCRUDService;
    private PageCRUDService pageCRUDService;
    @Getter
    @Setter
    private Site site;
    //private ArrayList<PageToDto> pagesListForSave = new ArrayList<>();

    public ThreadForSearchLinks(SiteCRUDService siteCRUDService,
                                PageCRUDService pageCRUDService,
                                Site site) {
        this.siteCRUDService = siteCRUDService;
        this.pageCRUDService = pageCRUDService;
        this.site = site;
    }

    @Override
    public void run() {
        try {
            String urlAndRoot = site.getUrl();
            Integer siteId = siteCRUDService.getIdByUrl(site.getUrl());
            if (siteId != null) {
                siteCRUDService.deleteById(siteId);
            }

            SiteEntity newSite = new SiteEntity();
            newSite.setUrl(site.getUrl());
            newSite.setName(site.getName());
            newSite.setStatusIndexing(StatusIndexing.INDEXING);
            siteCRUDService.save(newSite);
            Link.getRootUrls().add(site.getUrl());

            isUrlWorking(newSite.getUrl());
            Integer newSiteId = siteCRUDService.getIdByUrl(urlAndRoot);
            Link newLink = new Link(urlAndRoot, newSiteId, urlAndRoot);
            ParseWebRecursive newTask = new ParseWebRecursive(siteCRUDService, pageCRUDService);
            newTask.setLink(newLink);
            newTask.fork();
            Logger.getLogger(String.valueOf(ThreadForSearchLinks.class))
                    .info("ThreadForSearchLinks  " + site.getUrl() + " начался обход сайта");

            newTask.join();
            Thread.sleep(3100);
            Logger.getLogger(String.valueOf(ThreadForSearchLinks.class))
                    .info("Поиск по ссылке " + urlAndRoot + " завершен");
            ArrayList<PageToDto> pagesListForSave = (ArrayList<PageToDto>) PageToDto.getPageToDtoList().clone();
            //synchronized (PageToDto.getPageToDtoList()) {
                pageCRUDService.saveAndRemove(pagesListForSave);
            //}
            newSite.setStatusIndexing(StatusIndexing.INDEXED);
            newSite.setStatusTime(LocalDateTime.now());
            siteCRUDService.save(newSite);
            Logger.getLogger(String.valueOf(ThreadForSearchLinks.class))
                    .info("Статус индексации ссылки " + urlAndRoot + " изменен");
        } catch (HttpStatusException ex) {
            siteCRUDService.updateWithFailedStatus(site.getUrl(), getHttpStatusException(ex.getStatusCode()));
        } catch (IOException ex) {
            siteCRUDService.updateWithFailedStatus(site.getUrl(), "Отсутствует соединение");
        } catch (InterruptedException ex) {
            siteCRUDService.updateWithFailedStatus(site.getUrl(), "Индексация остановлена пользователем");
            Logger.getLogger(String.valueOf(IndexingService.class))
                    .info("Поиск по ссылке " + site.getUrl() + " прерван");
        }
    }

    public void isUrlWorking(String url) throws IOException {
        ConnectionWeb connection = new ConnectionWeb();
        connection.getDocument(url);
    }

    public String getHttpStatusException(Integer code) {
        String error;
        switch (code) {
            case (403) -> error = "Исключение HttpStatusException: ошибка HTTP при получении URL-адреса. " +
                    "Статус = 403. Доступ к сайту запрещен";
            case (404) -> error = "Исключение HttpStatusException: ошибка HTTP при получении URL-адреса. " +
                    "Статус = 404. Сервер не может найти нужную страницу";
            default -> error = "Исключение HttpStatusException. Статус = " + code;
        }
        return error;
    }
}
