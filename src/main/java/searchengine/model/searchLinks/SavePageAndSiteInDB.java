package searchengine.model.searchLinks;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import searchengine.dto.entityesToDto.PageToDto;
import searchengine.model.entityes.SiteEntity;
import searchengine.services.PageCRUDService;
import searchengine.services.SiteCRUDService;

import javax.persistence.criteria.CriteriaBuilder;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static searchengine.dto.entityesToDto.PageToDto.getPageToDtoList;
import static searchengine.dto.entityesToDto.PageToDto.setPageToDtoList;

@Component
public class SavePageAndSiteInDB implements Runnable {
    private JdbcTemplate jdbcTemplate;
    private SiteCRUDService siteCRUDService;
    private PageCRUDService pageCRUDService;
/*
    @Getter
    @Setter
    private static boolean flag;
*/

    @Getter
    @Setter
    private volatile static ArrayList<PageToDto> pageToDtoArrayList = new ArrayList<>();

    @Autowired
    public SavePageAndSiteInDB(SiteCRUDService siteCRUDService,
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
            } catch (InterruptedException e) {
                Logger.getLogger("SavePageAndSiteInDB")
                        .info("Прерван параллельный поток в состоянии ожидания");
                break;
            }
            pageToDtoArrayList = (ArrayList<PageToDto>) PageToDto.getPageToDtoList().clone();
            Logger.getLogger("SavePageAndSiteInDB")
                    .info("Размер эррэйлиста " + pageToDtoArrayList.size());
            if (pageToDtoArrayList.size() > 100) {
                pageCRUDService.saveAll(pageToDtoArrayList);
                Logger.getLogger("SavePageAndSiteInDB")
                        .info("Произведено сохранение PageToDTOList в DB size " + pageToDtoArrayList.size());
                Logger.getLogger("SavePageAndSiteInDB")
                        .info("Текущий размер PageToDTOList  " + PageToDto.getPageToDtoList().size());
                PageToDto.removePagesToDtoFromList(pageToDtoArrayList.size());
                Logger.getLogger("SavePageAndSiteInDB")
                        .info("Произведено исключение объектов PageToDTOList. Размеро PageToDTOList после исключения " + PageToDto.getPageToDtoList().size());
                for (Integer siteId : getAllSiteId()) {
                    SiteEntity siteEntity = siteCRUDService.getById(siteId);
                    siteEntity.setStatusTime(LocalDateTime.now());
                    siteCRUDService.save(siteEntity);
                }
                Logger.getLogger("SavePageAndSiteInDB")
                        .info("Прошел очередной цикл");
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

    public static void removePagesToDtoFromList(int count) {
        for (int i = 0; i < count; i++) {
            pageToDtoArrayList.remove(0);
        }
    }
}

