package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import searchengine.model.entityes.SiteEntity;
import searchengine.model.entityes.StatusIndexing;
import searchengine.repository.SiteRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SiteCRUDService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private SiteRepository siteRepository;

    @Autowired
    public SiteCRUDService(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    public SiteEntity getById(Integer id) {
        Optional<SiteEntity> site = siteRepository.findById(id);
        return site.orElse(null);
    }
    public Integer getIdByUrl(String url) {
        //TODO здесь переписать код. Запрос попробовать прописать в репозитории через анотацию Query
       List<SiteEntity> siteList = jdbcTemplate.query(
               "SELECT * FROM search_engine.site where url = ?", (rs, rowNum) -> {
                   SiteEntity siteEntity = new SiteEntity();
                   siteEntity.setId(rs.getInt("id"));
                   return siteEntity;
               }, url);
        if (siteList.isEmpty()) {
            return null;
        }
        return siteList.get(0).getId();
    }

    public void deleteById(Integer id) {
        siteRepository.deleteById(id);
    }

    public void save(SiteEntity site) {
        siteRepository.save(site);
    }

    public SiteEntity getByUrl(String url) {
        List<SiteEntity> siteEntityList = siteRepository.findAllByUrl(url);
        if (siteEntityList.isEmpty()) {
            return null;
        }
        return siteEntityList.get(0);
    }

    public String getStatusIndexing(int siteId) {
        return siteRepository.getStatusIndexing(siteId);
    }
    public String getErrorIndexing(int siteId){
        return siteRepository.getErrorIndexing(siteId);
    }

    public LocalDateTime getStatusTimeIndexing(int siteId) {
        return siteRepository.getStatusTimeIndexing(siteId);
    }

    public void updateWithFailedStatus(String url, String error) {
        SiteEntity siteEntity = getByUrl(url);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteEntity.setStatusIndexing(StatusIndexing.FAILED);
        siteEntity.setLastError(error);
        save(siteEntity);
    }
}
