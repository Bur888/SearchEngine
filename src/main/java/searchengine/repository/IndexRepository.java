package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.entityes.IndexEntity;

import java.util.List;


@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    @Query(value = "SELECT * FROM search_engine.index i WHERE i.page_id = :pageId and i.lemma_id = :lemmaId", nativeQuery = true)
    IndexEntity findOneByPageIdAndLemmaId(@Param("pageId") int pageId, @Param("lemmaId") int lemmaId);

    @Query(value = "SELECT * FROM search_engine.index i WHERE i.page_id = :pageId AND i.lemma_id = :lemmaId", nativeQuery = true)
    IndexEntity getPageIdAndLemmaIdInDB(@Param("pageId") int pageId, @Param("lemmaId") int lemmaId);
}
