package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import searchengine.dto.entityesToDto.PageToDto;
import searchengine.model.entityes.PageEntity;
import searchengine.model.entityes.SiteEntity;
import searchengine.model.searchLinks.Link;
import searchengine.repository.PageRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.LogManager;
import java.util.logging.Logger;


@Service
public class PageCRUDService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private SiteCRUDService siteCRUDService;
    private PageRepository pageRepository;

    @Autowired
    public PageCRUDService(SiteCRUDService siteCRUDService, PageRepository pageRepository) {
        this.siteCRUDService = siteCRUDService;
        this.pageRepository = pageRepository;
    }

    public PageEntity getById(Integer id) {
        Optional<PageEntity> pageOptional = pageRepository.findById(id);
        if (pageOptional.isPresent()) {
            return pageOptional.get();
        }
        return null;
    }

    public void savePage(PageToDto pageToDto) {
        PageEntity page = mapToEntity(pageToDto);
        pageRepository.save(page);
    }

    public boolean isUrlInDB(String url, Integer siteId) {
        List<PageToDto> pageToDtoList = jdbcTemplate.query(
                "SELECT * FROM search_engine.page WHERE path = ? AND site_id = ?",
                new Object[]{url, siteId},
                new RowMapper<PageToDto>() {
                    @Override
                    public PageToDto mapRow(ResultSet rs, int rowNum) throws SQLException {
                        PageToDto pageToDto = new PageToDto();
                        pageToDto.setId(rs.getInt("id"));
                        return pageToDto;
                    }
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

    public PageEntity mapToEntity(PageToDto pageToDto) {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setId(pageEntity.getId());
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
        ArrayList<PageToDto> pageToDtoArrayList = (ArrayList<PageToDto>) list.clone();
        Logger.getLogger("SavePageAndSiteInDB")
                .info("Размер эррэйлиста " + pageToDtoArrayList.size());
        saveAll(pageToDtoArrayList);
        Logger.getLogger("SavePageAndSiteInDB")
                .info("Произведено сохранение PageToDTOList в DB size " + pageToDtoArrayList.size());
        Logger.getLogger("SavePageAndSiteInDB")
                .info("Текущий размер PageToDTOList  " + PageToDto.getPageToDtoList().size());
        PageToDto.removePagesToDtoFromList(pageToDtoArrayList.size());
        Logger.getLogger("SavePageAndSiteInDB")
                .info("Произведено исключение объектов PageToDTOList. Размеро PageToDTOList после исключения " + PageToDto.getPageToDtoList().size());
    }
}

