package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.dto.entityesToDto.PageToDto;
import searchengine.model.entityes.PageEntity;
import searchengine.model.entityes.SiteEntity;
import searchengine.repository.PageRepository;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PageCRUDService {

    private JdbcTemplate jdbcTemplate;
    private static SiteCRUDService siteCRUDService;
    private static PageRepository pageRepository;

    @Autowired
    public PageCRUDService(JdbcTemplate jdbcTemplate, SiteCRUDService siteCRUDService, PageRepository pageRepository) {
        this.jdbcTemplate = jdbcTemplate;
        PageCRUDService.siteCRUDService = siteCRUDService;
        PageCRUDService.pageRepository = pageRepository;
    }

    public PageEntity findById(Integer id) {
        Optional<PageEntity> pageOptional = pageRepository.findById(id);
        return pageOptional.orElse(null);
    }

    public List<PageEntity> findAllByIdIn(List<Integer> pagesId) {
        return pageRepository.findAllByIdIn(pagesId);
    }

    public ArrayList<PageEntity> findByLemmaId(int lemmaId) {
        return pageRepository.findByLemmaId(lemmaId);
    }

    public ArrayList<PageEntity> findByLemma(String lemma) {
        return pageRepository.findByLemma(lemma);
    }

    public ArrayList<PageEntity> findByLemmaAndSiteId(String lemma, Integer siteId) {
        return pageRepository.findByLemmaAndSiteId(lemma, siteId);
    }

    public void save(PageEntity pageEntity) {
        pageRepository.save(pageEntity);
    }

    public boolean isUrlInDB(String url, Integer siteId) {
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
        saveAll(list);
        PageToDto.removePagesToDtoFromHashMap(list);
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

    public Integer getCountPagesOnSite(String site) {
        int siteId = siteCRUDService.getIdByUrl(site);
        return pageRepository.getCountPagesOnSite(siteId);
    }

    public Integer getCountAllPages() {
        return pageRepository.getCountAllPages();
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
}

