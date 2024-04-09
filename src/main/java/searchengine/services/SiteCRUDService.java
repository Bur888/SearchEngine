package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import searchengine.model.entityes.SiteEntity;
import searchengine.repository.SiteRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

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
        if (site.isPresent()) {
            return site.get();
        }
        return null;
    }
    public Integer getIdByUrl(String url) {
       List<SiteEntity> siteList = jdbcTemplate.query(
               "SELECT * FROM search_engine.site where url = ?", new RowMapper<SiteEntity>() {
                   @Override
                   public SiteEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
                       SiteEntity siteEntity = new SiteEntity();
                       siteEntity.setId(rs.getInt("id"));
                       return siteEntity;
                   }
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
}
