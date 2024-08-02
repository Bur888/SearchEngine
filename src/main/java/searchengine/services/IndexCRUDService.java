package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.model.entityes.IndexEntity;
import searchengine.repository.IndexRepository;
import java.sql.PreparedStatement;
import java.util.HashSet;

@Service
public class IndexCRUDService {

    private final IndexRepository indexRepository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public IndexCRUDService(IndexRepository indexRepository,
                            JdbcTemplate jdbcTemplate) {
        this.indexRepository = indexRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(IndexEntity index) {
        indexRepository.save(index);
    }

    public void saveAll(HashSet<IndexEntity> indexes) {
        jdbcTemplate.batchUpdate("INSERT INTO search_engine.index (lemma_id, page_id, `rank`) " +
                        "VALUES (?, ?, ?)",
                indexes,
                indexes.size(),
                (PreparedStatement ps, IndexEntity indexEntity) -> {
                    ps.setInt(1, indexEntity.getLemma().getId());
                    ps.setInt(2, indexEntity.getPage().getId());
                    ps.setFloat(3, indexEntity.getRank());
                });
    }

    public boolean containsPageIdAndLemmaIdInDB(int pageId, int lemmaId) {
        return indexRepository.getPageIdAndLemmaIdInDB(pageId, lemmaId) != null;
    }

    public IndexEntity findByPageIdAndLemmaId(int pageId, int lemmaId) {
        return indexRepository.findOneByPageIdAndLemmaId(pageId, lemmaId);
    }
}
