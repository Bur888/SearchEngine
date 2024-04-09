package searchengine.services;

import org.apache.tomcat.util.digester.ArrayStack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.IndexingResponseFalse;
import searchengine.dto.indexing.IndexingResponseTrue;
import searchengine.model.entityes.SiteEntity;
import searchengine.model.entityes.StatusIndexing;
import searchengine.model.searchLinks.Link;
import searchengine.model.searchLinks.ParseWebRecursive;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.logging.Logger;

@Service
public class IndexingService {

    private static boolean startIndexing = false;
    @Autowired
    private SiteCRUDService siteCRUDService;
    @Autowired
    private PageCRUDService pageCRUDService;
    @Autowired
    private SitesList sitesList;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private ArrayList<ParseWebRecursive> tasks = new ArrayList<>();

    public IndexingResponse startIndexing() {
        if (startIndexing == true) {
            return new IndexingResponseFalse();
        }
        int i = 1;
       // ArrayList<ParseWebRecursive> taskList = new ArrayList<>();
        startIndexing = true;
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
        startIndexing = false;
        return new IndexingResponseTrue();
    }
}

