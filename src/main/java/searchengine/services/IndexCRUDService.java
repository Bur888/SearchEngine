package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.dto.entityesToDto.IndexToDto;
import searchengine.model.entityes.IndexEntity;
import searchengine.model.entityes.LemmaEntity;
import searchengine.model.entityes.PageEntity;
import searchengine.repository.IndexRepository;

import java.sql.PreparedStatement;
import java.util.HashSet;
import java.util.List;

@Service
public class IndexCRUDService {

    private final PageCRUDService pageCRUDService;

    private final IndexRepository indexRepository;

    private final LemmaCRUDService lemmaCRUDService;
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public IndexCRUDService(PageCRUDService pageCRUDService,
                            IndexRepository indexRepository,
                            LemmaCRUDService lemmaCRUDService,
                            JdbcTemplate jdbcTemplate) {
        this.pageCRUDService = pageCRUDService;
        this.indexRepository = indexRepository;
        this.lemmaCRUDService = lemmaCRUDService;
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

    public IndexToDto mapToDto(IndexEntity index) {
        IndexToDto indexToDto = new IndexToDto();
        indexToDto.setId(index.getId());
        indexToDto.setPageID(index.getPage().getId());
        indexToDto.setLemmaId(index.getLemma().getId());
        indexToDto.setRank(index.getRank());
        return indexToDto;
    }

    public IndexEntity mapToEntity(IndexToDto indexToDto) {
        IndexEntity index = new IndexEntity();
        index.setId(indexToDto.getId());
        PageEntity page = pageCRUDService.findById(indexToDto.getId());
        index.setPage(page);
        LemmaEntity lemma = lemmaCRUDService.findById(indexToDto.getLemmaId());
        index.setLemma(lemma);
        index.setRank(indexToDto.getRank());
        return index;
    }

    public IndexEntity makeIndexEntity(LemmaEntity lemma,
                                              PageEntity pageEntity,
                                              Integer countThisWord,
                                              Integer countWordOnPage) {
        IndexEntity index = new IndexEntity();
        index.setLemma(lemma);
        index.setPage(pageEntity);
        float rank = IndexEntity.calculateRankValue(countThisWord, countWordOnPage);
        index.setRank(rank);
        return index;
    }

}
