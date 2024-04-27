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


public class ThreadForSavePageAndSiteInDB implements Runnable {
    private JdbcTemplate jdbcTemplate;
    private SiteCRUDService siteCRUDService;
    private PageCRUDService pageCRUDService;
    @Getter
    @Setter
    private volatile static ArrayList<PageToDto> pageToDtoArrayList = new ArrayList<>();

    @Autowired
    public  ThreadForSavePageAndSiteInDB(SiteCRUDService siteCRUDService,
                                        PageCRUDService pageCRUDService,
                                        JdbcTemplate jdbcTemplate) {
        this.siteCRUDService = siteCRUDService;
        this.jdbcTemplate = jdbcTemplate;
        this.pageCRUDService = pageCRUDService;
    }

    @Override
    public void run() {
        Logger.getLogger("ThreadForSavePageAndSiteInDB")
                .info("Параллельный поток пошел");
        while (true) {
            try {
                Thread.sleep(3000);
            if (PageToDto.getPageToDtoList().size() > 100) {
                pageToDtoArrayList = (ArrayList<PageToDto>) PageToDto.getPageToDtoList().clone();
                //synchronized (ThreadForSavePageAndSiteInDB.class) {
                    pageCRUDService.saveAndRemove(pageToDtoArrayList);
                //}
                for (Integer siteId : getAllSiteId()) {
                    SiteEntity siteEntity = siteCRUDService.getById(siteId);
                    siteEntity.setStatusTime(LocalDateTime.now());
                    siteCRUDService.save(siteEntity);
                }
                Link.removeAllLinks(pageToDtoArrayList);
                pageToDtoArrayList.clear();
                Logger.getLogger("SavePageAndSiteInDB")
                        .info("Прошел очередной цикл");
            }
            } catch (InterruptedException e) {
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

