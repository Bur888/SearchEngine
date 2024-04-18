package searchengine.services;

import org.apache.tomcat.util.digester.ArrayStack;
import org.jsoup.HttpStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.entityesToDto.PageToDto;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.IndexingResponseFalse;
import searchengine.dto.indexing.IndexingResponseTrue;
import searchengine.model.entityes.SiteEntity;
import searchengine.model.entityes.StatusIndexing;
import searchengine.model.searchLinks.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

@Service
public class IndexingService {

    private static boolean startIndexing;
    @Autowired
    private SiteCRUDService siteCRUDService;
    @Autowired
    private PageCRUDService pageCRUDService;
    @Autowired
    private SitesList sitesList;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private SavePageAndSiteInDB savePageAndSiteInDB;
    private ArrayList<ParseWebRecursive> tasks = new ArrayList<>();

    public IndexingResponse startIndexing() {

        if (startIndexing == true) {
            return new IndexingResponseFalse();
        }

        startIndexing = true;
        Thread forSavePageAndSiteInDB = new Thread(new SavePageAndSiteInDB(siteCRUDService, pageCRUDService, jdbcTemplate));
        forSavePageAndSiteInDB.start();

        for (Site site : sitesList.getSites()) {
            String url = site.getUrl();
            Integer siteId = siteCRUDService.getIdByUrl(url);
            if (siteId != null) {
                siteCRUDService.deleteById(siteId);
            }

            SiteEntity newSite = new SiteEntity();
            newSite.setUrl(site.getUrl());
            newSite.setName(site.getName());
            newSite.setStatusIndexing(StatusIndexing.INDEXING);
            siteCRUDService.save(newSite);
            try {
                isUrlWorking(newSite.getUrl());
            } catch (HttpStatusException ex) {
                siteCRUDService.updateWithFailedStatus(newSite.getUrl(), getHttpStatusException(ex.getStatusCode()));
                break;
            } catch (IOException ex) {
                siteCRUDService.updateWithFailedStatus(newSite.getUrl(), "Отсутствует соединение");
                break;
            }

            Integer newSiteId = siteCRUDService.getIdByUrl(url);
            Link newLink = new Link(url, newSiteId);
            ParseWebRecursive newTask = new ParseWebRecursive(siteCRUDService, pageCRUDService);
            newTask.setLink(newLink);
            newTask.fork();
            tasks.add(newTask);
            Logger.getLogger(String.valueOf(IndexingService.class))
                    .info("IndexingService  " + site.getUrl() + " включен в списко задач");
        }

        for (ParseWebRecursive task : tasks) {
            task.join();
            Logger.getLogger(String.valueOf(IndexingService.class))
                            .info("Поиск по ссылке " + task.getLink().getUrl() + " завершен");
            SiteEntity newSiteEntity = siteCRUDService.getById(task.getLink().getSiteId());
            newSiteEntity.setStatusIndexing(StatusIndexing.INDEXED);
            siteCRUDService.save(newSiteEntity);
            Logger.getLogger(String.valueOf(IndexingService.class))
                            .info("Статус индексации ссылки " + task.getLink().getUrl() + " изменен");
        }

        if (!PageToDto.getPageToDtoList().isEmpty()) {
            pageCRUDService.saveAll(PageToDto.getPageToDtoList());
            PageToDto.getPageToDtoList().removeAll(PageToDto.getPageToDtoList());
            SavePageAndSiteInDB.setFlag(false);
            try {
                forSavePageAndSiteInDB.join();
                Logger.getLogger(String.valueOf(IndexingService.class))
                                .info("Последнее сохранение в базу данных завершено");
            } catch (InterruptedException e) {
                Logger.getLogger(String.valueOf(IndexingService.class))
                                .info("Прерван параллельный поток для сохранения Page и Site в базу данных");
            }
        }

        forSavePageAndSiteInDB.interrupt();
        PageToDto.getPageToDtoList().clear();
        Link.getAllLinks().clear();
        startIndexing = false;
        return new IndexingResponseTrue();
    }

    public void isUrlWorking(String url) throws IOException {
        ConnectionWeb connection = new ConnectionWeb();
        connection.getDocument(url);
    }

    public static String getHttpStatusException(Integer code) {
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

