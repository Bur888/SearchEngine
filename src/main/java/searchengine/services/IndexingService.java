package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.entityesToDto.PageToDto;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.IndexingResponseFalse;
import searchengine.dto.indexing.IndexingResponseTrue;
import searchengine.model.searchLinks.*;

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
    private ThreadForSavePageAndSiteInDB savePageAndSiteInDB;
    private ArrayList<ParseWebRecursive> tasks = new ArrayList<>();
    private ArrayList<Thread> threads = new ArrayList<>();
    private Thread forSavePageAndSiteInDB;

    public IndexingResponse startIndexing() {

        if (startIndexing == true) {
            return new IndexingResponseFalse("Индексация уже запущена");
        }

        startIndexing = true;
        forSavePageAndSiteInDB = new Thread(new ThreadForSavePageAndSiteInDB(siteCRUDService, pageCRUDService, jdbcTemplate));
        forSavePageAndSiteInDB.start();

        ArrayList<Thread> threads = new ArrayList<>();
        for (Site site : sitesList.getSites()) {
            Thread threadForSearchLinks = new Thread(new ThreadForSearchLinks(siteCRUDService, pageCRUDService, site));
            threadForSearchLinks.start();
            threads.add(threadForSearchLinks);
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Logger.getLogger(String.valueOf(IndexingService.class))
                        .info("Прерван параллельный поток для сохранения Page и Site в базу данных");
            }

        }

        forSavePageAndSiteInDB.interrupt();
        Logger.getLogger(String.valueOf(IndexingService.class))
                .info("Поток SavePageAndSiteInDB завершен");
        Logger.getLogger(String.valueOf(IndexingService.class))
                .info("Размер листа SavePageAndSiteInDB " + ThreadForSavePageAndSiteInDB.getPageToDtoArrayList().size());
        Logger.getLogger(String.valueOf(IndexingService.class))
                .info("Размер листа PageToDtoList " + PageToDto.getPageToDtoList().size());

        startIndexing = false;
        return new IndexingResponseTrue();
    }

    public IndexingResponse stopIndexing() {
        if (startIndexing == false) {
            return new IndexingResponseFalse("Индексация не запущена");
        }
        forSavePageAndSiteInDB.interrupt();
        for (Thread thread : threads) {
            thread.interrupt();
        }
        pageCRUDService.saveAndRemove(PageToDto.getPageToDtoList());
        return new IndexingResponseTrue();
    }
}


