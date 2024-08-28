package searchengine.model.searchLinks;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.dto.entityesToDto.PageToDto;
import searchengine.model.SavePageLemmaIndexInDb;
import searchengine.model.entityes.IndexEntity;
import searchengine.model.findAndSaveLemmaAndIndex.FindAndSaveLemmaAndIndex;
import searchengine.services.*;

import java.util.ArrayList;
import java.util.logging.Logger;

public class ThreadForSavePageAndSiteInDB implements Runnable {
    private final SiteCRUDService siteCRUDService;
    private final PageCRUDService pageCRUDService;
    private final LemmaCRUDService lemmaCRUDService;
    private final IndexCRUDService indexCRUDService;
    @Getter
    @Setter
    private volatile static ArrayList<PageToDto> pageToDtoArrayList = new ArrayList<>();

    @Autowired
    public ThreadForSavePageAndSiteInDB(SiteCRUDService siteCRUDService,
                                        PageCRUDService pageCRUDService,
                                        LemmaCRUDService lemmaCRUDService,
                                        IndexCRUDService indexCRUDService) {
        this.siteCRUDService = siteCRUDService;
        this.lemmaCRUDService = lemmaCRUDService;
        this.indexCRUDService = indexCRUDService;
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
                   SavePageLemmaIndexInDb saveAllInDb = new SavePageLemmaIndexInDb(pageCRUDService, siteCRUDService);
                    saveAllInDb.saveAllInDB(findAndSaveLemmaAndIndex, false);
                }
            } catch (InterruptedException e) {
                try {
                    Thread.sleep(2000);
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
}

