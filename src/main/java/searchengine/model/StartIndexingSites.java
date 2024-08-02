package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.config.SitesList;
import searchengine.dto.entityesToDto.PageToDto;
import searchengine.model.searchLinks.Link;
import searchengine.model.searchLinks.ThreadForSavePageAndSiteInDB;
import searchengine.model.searchLinks.ThreadForSearchLinks;
import searchengine.services.*;
import searchengine.config.Site;

import java.util.ArrayList;
import java.util.logging.Logger;

public class StartIndexingSites implements Runnable {

    private final SiteCRUDService siteCRUDService;
    private final PageCRUDService pageCRUDService;
    private final LemmaCRUDService lemmaCRUDService;
    private final IndexCRUDService indexCRUDService;
    private final SitesList sitesList;
    @Getter
    @Setter
    private static ArrayList<Thread> threads = new ArrayList<>();


    @Autowired
    public StartIndexingSites(SiteCRUDService siteCRUDService,
                              PageCRUDService pageCRUDService,
                              LemmaCRUDService lemmaCRUDService,
                              IndexCRUDService indexCRUDService,
                              SitesList sitesList) {
        this.siteCRUDService = siteCRUDService;
        this.pageCRUDService = pageCRUDService;
        this.lemmaCRUDService = lemmaCRUDService;
        this.indexCRUDService = indexCRUDService;
        this.sitesList = sitesList;

    }

    @Override
    public void run() {

        Thread forSavePageAndSiteInDB
                = new Thread(new ThreadForSavePageAndSiteInDB(siteCRUDService,
                                                              pageCRUDService,
                                                              lemmaCRUDService,
                                                              indexCRUDService));
        forSavePageAndSiteInDB.start();

        for (Site site : sitesList.getSites()) {
            Thread threadForSearchLinks
                    = new Thread(new ThreadForSearchLinks(siteCRUDService,
                                                          pageCRUDService,
                                                          lemmaCRUDService,
                                                          indexCRUDService,
                                                          site));
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
        Link.getAllLinks().clear();
        Logger.getLogger(String.valueOf(IndexingService.class))
                .info("Поток SavePageAndSiteInDB завершен");
        Logger.getLogger(String.valueOf(IndexingService.class))
                .info("Размер листа SavePageAndSiteInDB " + ThreadForSavePageAndSiteInDB.getPageToDtoArrayList().size());
        Logger.getLogger(String.valueOf(IndexingService.class))
                .info("Размер листа PageToDtoList " + PageToDto.getPageToDtoHashMap().size());

        IndexingService.setStartIndexingFlag(false);
    }
}
