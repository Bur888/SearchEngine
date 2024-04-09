package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.catalina.util.Introspection;
import org.aspectj.apache.bcel.classfile.Module;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSourceExtensionsKt;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import searchengine.dto.entityesToDto.PageToDto;
import searchengine.model.entityes.PageEntity;
import searchengine.model.entityes.SiteEntity;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;


@Service
public class PageCRUDService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private SiteCRUDService siteCRUDService;
    private PageRepository pageRepository;

    @Autowired
    public PageCRUDService(SiteCRUDService siteCRUDService, PageRepository pageRepository) {
        this.siteCRUDService =siteCRUDService;
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

    public boolean isUrlLInDB(String url, Integer siteId) {
/*
        Integer entity = jdbcTemplate.queryForObject(
                "SELECT * FROM search_engine.page WHERE path = ? AND site_id = " + siteId,
                Integer.class, url);
        return entity != null && entity > 0;
*/
/*
        List<PageToDto> pageToDtoList = jdbcTemplate.query(
                "SELECT * FROM search_engine.page WHERE path = :url AND site_id = :siteId",
                new MapSqlParameterSource()
                        .addValue("url", url)
                        .addValue("siteId", siteId),
                );
*/

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
        return pageToDtoList.isEmpty();
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
}
