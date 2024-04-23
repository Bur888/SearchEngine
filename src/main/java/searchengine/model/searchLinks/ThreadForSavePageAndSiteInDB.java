package searchengine.model.searchLinks;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import searchengine.dto.entityesToDto.PageToDto;
import searchengine.model.entityes.SiteEntity;
import searchengine.services.PageCRUDService;
import searchengine.services.SiteCRUDService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;

@Component
public class ThreadForSavePageAndSiteInDB implements Runnable {
    private JdbcTemplate jdbcTemplate;
    private SiteCRUDService siteCRUDService;
    private PageCRUDService pageCRUDService;

   // private static boolean flag;

    @Getter
    @Setter
    private volatile static ArrayList<PageToDto> pageToDtoArrayList = new ArrayList<>();

    @Autowired
    public ThreadForSavePageAndSiteInDB(SiteCRUDService siteCRUDService,
                                        PageCRUDService pageCRUDService,
                                        JdbcTemplate jdbcTemplate) {
        this.siteCRUDService = siteCRUDService;
        this.jdbcTemplate = jdbcTemplate;
        this.pageCRUDService = pageCRUDService;
    }

    @Override
    public void run() {
        Logger.getLogger("SavePageAndSiteInDB")
                .info("Параллельный поток пошел");
        //flag = true;
        while (true) {
            try {
                Thread.sleep(3000);
            if (PageToDto.getPageToDtoList().size() > 100) {
                synchronized (ThreadForSavePageAndSiteInDB.class) {
                    pageCRUDService.saveAndRemove(PageToDto.getPageToDtoList());
                }
                for (Integer siteId : getAllSiteId()) {
                    SiteEntity siteEntity = siteCRUDService.getById(siteId);
                    siteEntity.setStatusTime(LocalDateTime.now());
                    siteCRUDService.save(siteEntity);
                }
                pageToDtoArrayList.clear();
                Logger.getLogger("SavePageAndSiteInDB")
                        .info("Прошел очередной цикл");
            }
            } catch (InterruptedException e) {
                Logger.getLogger("SavePageAndSiteInDB")
                        .info("Прерван параллельный поток в состоянии ожидания");
                //flag = false;
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

