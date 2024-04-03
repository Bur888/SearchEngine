package searchengine.services;

import lombok.RequiredArgsConstructor;
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
        startIndexing = true;
        for (Site site : sitesList.getSites()) {
            String url = site.getUrl();
            Logger.getLogger(String.valueOf(IndexingService.class)).info("сайт " + url + " пошел");
            Integer siteId = siteCRUDService.getIdByUrl(url);
            if (!(siteId == null)) {
                siteCRUDService.deleteById(siteId);
            }

            SiteEntity newSite = new SiteEntity();
            newSite.setUrl(site.getUrl());
            newSite.setName(site.getName());
            newSite.setStatusIndexing(StatusIndexing.INDEXING);
            siteCRUDService.save(newSite);
            Logger.getLogger(String.valueOf(IndexingService.class)).info("После сохранением сайта в репозиторий" + url);

            Link newLink = new Link(url);
            ParseWebRecursive newTask = new ParseWebRecursive();
            newTask.setLink(newLink);
            newTask.setSiteEntity(newSite);
            newTask.fork();
            tasks.add(newTask);
            Logger.getLogger("Организован поиск по ссылке " + site.getUrl());
        }

        for (ParseWebRecursive task : tasks) {
            task.join();
        }
        startIndexing = false;
        return new IndexingResponseTrue();
    }
}

