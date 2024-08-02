package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.entityes.LemmaEntity;
import java.util.HashSet;
import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    @Query(value = "SELECT * FROM search_engine.lemma l WHERE l.lemma = ?1 and l.site_id = ?2 LIMIT 1", nativeQuery = true)
    LemmaEntity findOneByLemmaAndSiteId(String lemma, int siteId);

    List<LemmaEntity> findAllByLemma(String lemma);

    List<LemmaEntity> findAllBySiteId(int siteId);

    @Query(value = "SELECT * FROM search_engine.lemma l WHERE l.lemma = ?1 and l.site_id = ?2", nativeQuery = true)
    int findIdBySite(String site);

    @Query(value = "SELECT l.* FROM search_engine.lemma l " +
            "JOIN search_engine.index i ON i.lemma_id = l.id " +
            "WHERE i.page_id = ?1", nativeQuery = true)
    List<LemmaEntity> findLemmasByPageId(int pageId);

    @Query(value = "UPDATE lemma SET search_engine.lemma l WHERE l.lemma = ?1 and l.site_id = ?2", nativeQuery = true)
    void saveAll(HashSet<LemmaEntity> lemmas);

    @Query(value = "SELECT COUNT(*) FROM search_engine.lemma l WHERE l.site_id = :siteId", nativeQuery = true)
    Integer getCountLemmasOnSite(@Param("siteId") int siteId);

    @Query(value = "SELECT l.frequency FROM search_engine.lemma l WHERE l.lemma = ?1", nativeQuery = true)
    Integer getCountPagesWithLemma(String lemma);
}
