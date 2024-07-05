package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import searchengine.dto.entityesToDto.PageToDto;
import searchengine.model.entityes.PageEntity;
import searchengine.model.entityes.SiteEntity;
import searchengine.repository.PageRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;


@Service
public class PageCRUDService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static SiteCRUDService siteCRUDService;
    private static PageRepository pageRepository;

    @Autowired
    public PageCRUDService(SiteCRUDService siteCRUDService, PageRepository pageRepository) {
        PageCRUDService.siteCRUDService = siteCRUDService;
        PageCRUDService.pageRepository = pageRepository;
    }

    public PageEntity findById(Integer id) {
        Optional<PageEntity> pageOptional = pageRepository.findById(id);
        return pageOptional.orElse(null);
    }

    public void save(PageEntity pageEntity) {
        pageRepository.save(pageEntity);
    }

    public boolean isUrlInDB(String url, Integer siteId) {
        //TODO здесь переписать код. Запрос попробовать прописать в репозитории через анотацию Query
        List<PageToDto> pageToDtoList = jdbcTemplate.query(
                "SELECT * FROM search_engine.page WHERE path = ? AND site_id = ?",
                new Object[]{url, siteId},
                (rs, rowNum) -> {
                    PageToDto pageToDto = new PageToDto();
                    pageToDto.setId(rs.getInt("id"));
                    return pageToDto;
                }
        );
        return !pageToDtoList.isEmpty();
    }

    public static PageToDto mapToDto(PageEntity pageEntity) {
        PageToDto pageToDto = new PageToDto();
        pageToDto.setId(pageEntity.getId());
        pageToDto.setSiteId(pageEntity.getSite().getId());
        pageToDto.setPath(pageEntity.getPath());
        pageToDto.setCode(pageEntity.getCode());
        pageToDto.setContent(pageEntity.getContent());
        return pageToDto;
    }

    public static PageEntity mapToEntity(PageToDto pageToDto) {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setId(pageToDto.getId());
        SiteEntity siteEntity = siteCRUDService.getById(pageToDto.getSiteId());
        pageEntity.setSite(siteEntity);
        pageEntity.setPath(pageToDto.getPath());
        pageEntity.setContent(pageToDto.getContent());
        pageEntity.setCode(pageToDto.getCode());
        return pageEntity;
    }

    public void saveAll(ArrayList<PageToDto> pageList) {
        jdbcTemplate.batchUpdate("INSERT INTO search_engine.page (site_id, path, code, content) " +
                        "VALUES (?, ?, ?, ?)",
                pageList,
                pageList.size(),
                (PreparedStatement ps, PageToDto pageToDto) -> {
                    ps.setInt(1, pageToDto.getSiteId());
                    ps.setString(2, pageToDto.getPath());
                    ps.setInt(3, pageToDto.getCode());
                    ps.setString(4, pageToDto.getContent());
                });
    }
    public void saveAndRemove(ArrayList<PageToDto> list) {
        Logger.getLogger("PageCRUDService")
                .info("Размер эррэйлиста " + list.size());
        saveAll(list);
        Logger.getLogger("PageCRUDService")
                .info("Произведено сохранение PageToDTOList в DB size " + list.size());
        Logger.getLogger("PageCRUDService")
                .info("Текущий размер PageToDTOList  " + PageToDto.getPageToDtoHashMap().size());
        PageToDto.removePagesToDtoFromHashMap(list);
        Logger.getLogger("SavePageAndSiteInDBPageCRUDService")
                .info("Произведено исключение объектов PageToDTOList. Размеро PageToDTOList после исключения " + PageToDto.getPageToDtoHashMap().size());
    }

    public PageEntity findOneByPathAndSiteId(String url, int siteId) {
        return pageRepository.findOneByPathAndSiteId(url, siteId);
    }

    public void delete(PageEntity pageEntity) {
        pageRepository.delete(pageEntity);
    }

    public List<PageEntity> findAllMoreThenStartId(int startId) {
        return pageRepository.findAllMoreThenStartId(startId);
    }

    public List<PageEntity> findAll() {
        return pageRepository.findAll();
    }

    public Integer getCountPagesOnSite(int siteId) {
        return pageRepository.getCountPagesOnSite(siteId);
    }
}

