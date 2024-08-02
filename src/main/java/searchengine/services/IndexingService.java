package searchengine.services;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.IndexingResponseFalse;
import searchengine.dto.indexing.IndexingResponseTrue;
import searchengine.model.IndexPage;
import searchengine.model.StartIndexingSites;
import searchengine.model.searchLinks.*;


@Service
public class IndexingService {

    @Getter
    @Setter
    private static boolean startIndexingFlag;
    private final SiteCRUDService siteCRUDService;
    private final PageCRUDService pageCRUDService;
    private final LemmaCRUDService lemmaCRUDService;
    private final IndexCRUDService indexCRUDService;
    private final SitesList sitesList;
    private Thread startIndexing;

    @Autowired
    public IndexingService(SiteCRUDService siteCRUDService, PageCRUDService pageCRUDService, LemmaCRUDService lemmaCRUDService, IndexCRUDService indexCRUDService, SitesList sitesList) {
        this.siteCRUDService = siteCRUDService;
        this.pageCRUDService = pageCRUDService;
        this.lemmaCRUDService = lemmaCRUDService;
        this.indexCRUDService = indexCRUDService;
        this.sitesList = sitesList;
    }

    public IndexingResponse startIndexing() {

        if (startIndexingFlag) {
            return new IndexingResponseFalse("Индексация уже запущена");
        }
        ParseWebRecursive.setStopNow(false);
        startIndexingFlag = true;
        startIndexing = new Thread(new StartIndexingSites(siteCRUDService,
                pageCRUDService,
                lemmaCRUDService,
                indexCRUDService,
                sitesList));
        startIndexing.start();
        return new IndexingResponseTrue();
    }

    public IndexingResponse stopIndexing() {
        if (!startIndexingFlag) {
            return new IndexingResponseFalse("Индексация не запущена");
        }
        ParseWebRecursive.setStopNow(true);
        for (Thread thread : StartIndexingSites.getThreads()) {
            thread.interrupt();
        }

        startIndexing.interrupt();
        startIndexingFlag = false;
        return new IndexingResponseTrue();
    }

    public IndexingResponse indexPage(String url) {
        IndexPage indexPage = new IndexPage(siteCRUDService,
                                            pageCRUDService,
                                            lemmaCRUDService,
                                            indexCRUDService,
                                            sitesList);
        return indexPage.getResponse(url);
    }
}

