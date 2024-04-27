package searchengine.services.indexingService;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.entityesToDto.PageToDto;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.IndexingResponseFalse;
import searchengine.dto.indexing.IndexingResponseTrue;
import searchengine.model.searchLinks.*;
import searchengine.services.PageCRUDService;
import searchengine.services.SiteCRUDService;

import java.util.ArrayList;


@Service
public class IndexingService {

    @Getter
    @Setter
    private static boolean startIndexingFlag;
    @Autowired
    private SiteCRUDService siteCRUDService;
    @Autowired
    private PageCRUDService pageCRUDService;
    @Autowired
    private SitesList sitesList;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private ThreadForSavePageAndSiteInDB savePageAndSiteInDB;
    private ArrayList<ParseWebRecursive> tasks = new ArrayList<>();
    private Thread startIndexing;

    public IndexingResponse startIndexing() {

        if (startIndexingFlag == true) {
            return new IndexingResponseFalse("Индексация уже запущена");
        }
        ParseWebRecursive.setStopNow(false);
        startIndexingFlag = true;
        startIndexing = new Thread(new StartIndexing(siteCRUDService, pageCRUDService, jdbcTemplate, sitesList));
        startIndexing.start();
        return new IndexingResponseTrue();
    }

    public IndexingResponse stopIndexing() {
        if (startIndexingFlag == false) {
            return new IndexingResponseFalse("Индексация не запущена");
        }
        ParseWebRecursive.setStopNow(true);
        for (Thread thread : StartIndexing.getThreads()) {
            thread.interrupt();
        }

        startIndexing.interrupt();
/*
        try {
            Thread.sleep(3200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
*/
        PageToDto.getPageToDtoList().clear();
        Link.getAllLinks().clear();
        startIndexingFlag = false;
        return new IndexingResponseTrue();
    }
}


