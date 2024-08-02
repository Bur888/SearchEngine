package searchengine.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.dto.entityesToDto.PageToDto;
import searchengine.model.entityes.SiteEntity;
import searchengine.model.findAndSaveLemmaAndIndex.FindAndSaveLemmaAndIndex;
import searchengine.model.searchLinks.Link;
import searchengine.model.searchLinks.ThreadForSearchLinks;
import searchengine.services.PageCRUDService;
import searchengine.services.SiteCRUDService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;

public class SaveAllInDb {
    private static PageCRUDService pageCRUDService;
    private static SiteCRUDService siteCRUDService;
    private static ArrayList<PageToDto> pageToDtoArrayList = new ArrayList<>();

    @Autowired
    public SaveAllInDb(PageCRUDService pageCRUDService,
                       SiteCRUDService siteCRUDService) {
        SaveAllInDb.pageCRUDService = pageCRUDService;
        SaveAllInDb.siteCRUDService = siteCRUDService;
    }

    public static synchronized void saveAllInDB(FindAndSaveLemmaAndIndex findAndSaveLemmaAndIndex, boolean isFinishSaveOnSite) {
        Logger.getLogger(String.valueOf(ThreadForSearchLinks.class))
                .info("ссылка " + Thread.currentThread() + " зашла в синхронизированный поток");
        pageToDtoArrayList = PageToDto.getPageToDtoArrayList();
        pageCRUDService.saveAndRemove(pageToDtoArrayList);

        if(!isFinishSaveOnSite) {
            for (Integer siteId : getAllSiteId()) {
                SiteEntity siteEntity = siteCRUDService.getById(siteId);
                siteEntity.setStatusTime(LocalDateTime.now());
                siteCRUDService.save(siteEntity);
            }
        }

        Link.removeAllLinks(pageToDtoArrayList);
        findAndSaveLemmaAndIndex.run();
    }

    public static HashSet<Integer> getAllSiteId() {
        int siteId;
        HashSet<Integer> uniqSiteId = new HashSet<>();
        for (PageToDto page : pageToDtoArrayList) {
            siteId = page.getSiteId();
            uniqSiteId.add(siteId);
        }
        return uniqSiteId;
    }
}
