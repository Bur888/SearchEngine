package searchengine.model.searchLinks;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import searchengine.dto.entityesToDto.PageToDto;
import searchengine.model.SaveAllInDb;
import searchengine.model.entityes.IndexEntity;
import searchengine.model.entityes.SiteEntity;
import searchengine.model.findAndSaveLemmaAndIndex.FindAndSaveLemmaAndIndex;
import searchengine.services.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;

public class ThreadForSavePageAndSiteInDB implements Runnable {
    private JdbcTemplate jdbcTemplate;
    private SiteCRUDService siteCRUDService;
    private PageCRUDService pageCRUDService;
    private LemmaCRUDService lemmaCRUDService;
    private IndexCRUDService indexCRUDService;
    @Getter
    @Setter
    private volatile static ArrayList<PageToDto> pageToDtoArrayList = new ArrayList<>();

    @Autowired
    public ThreadForSavePageAndSiteInDB(SiteCRUDService siteCRUDService,
                                        PageCRUDService pageCRUDService,
                                        LemmaCRUDService lemmaCRUDService,
                                        IndexCRUDService indexCRUDService,
                                        JdbcTemplate jdbcTemplate) {
        this.siteCRUDService = siteCRUDService;
        this.lemmaCRUDService = lemmaCRUDService;
        this.indexCRUDService = indexCRUDService;
        this.jdbcTemplate = jdbcTemplate;
        this.pageCRUDService = pageCRUDService;
    }

    @Override
    public void run() {
        Logger.getLogger("ThreadForSavePageAndSiteInDB")
                .info("Параллельный поток пошел");
        FindAndSaveLemmaAndIndex findAndSaveLemmaAndIndex
                = new FindAndSaveLemmaAndIndex(pageCRUDService, lemmaCRUDService, indexCRUDService);
        while (true) {
            try {
                Thread.sleep(3000);
                if (PageToDto.getPageToDtoHashMap().size() > 100) {
                    SaveAllInDb saveAllInDb = new SaveAllInDb(pageCRUDService, siteCRUDService);
                    saveAllInDb.saveAllInDB(findAndSaveLemmaAndIndex, false);
/*
                    synchronized (PageToDto.getPageToDtoArrayList()) {
                        pageToDtoArrayList = PageToDto.getPageToDtoArrayList();
                        pageCRUDService.saveAndRemove(pageToDtoArrayList);

                        for (Integer siteId : getAllSiteId()) {
                            SiteEntity siteEntity = siteCRUDService.getById(siteId);
                            siteEntity.setStatusTime(LocalDateTime.now());
                            siteCRUDService.save(siteEntity);
                        }

                        Link.removeAllLinks(pageToDtoArrayList);
                        pageToDtoArrayList.clear();
                        Logger.getLogger("SavePageAndSiteInDB")
                                .info("Прошел очередной цикл");

                        findAndSaveLemmaAndIndex.run();
                    }
*/
                }
            } catch (InterruptedException e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                PageToDto.getPageToDtoHashMap().clear();
                Link.getAllLinks().clear();
                FindAndSaveLemmaAndIndex.setFinishSave(0);
                FindAndSaveLemmaAndIndex.setNUM(0);
                IndexEntity.getIndexes().clear();
                ThreadForSavePageAndSiteInDB.getPageToDtoArrayList().clear();
                FindAndSaveLemmaAndIndex.getLemmasExistingInDB().clear();
                FindAndSaveLemmaAndIndex.getLemmasNotExistingInDB().clear();
                Logger.getLogger("ThreadForSavePageAndSiteInDB")
                        .info("Прерван параллельный поток в состоянии ожидания");
                break;
            }
        }
    }

    public HashSet<Integer> getAllSiteId() {
        int siteId;
        HashSet<Integer> uniqSiteId = new HashSet<>();
        for (PageToDto page : pageToDtoArrayList) {
            siteId = page.getSiteId();
            uniqSiteId.add(siteId);
        }
        return uniqSiteId;
    }
}

