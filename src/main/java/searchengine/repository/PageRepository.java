package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.entityes.PageEntity;

import java.util.ArrayList;
import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    @Query(value = "SELECT * FROM search_engine.page p WHERE p.path = ?1 and p.site_id = ?2", nativeQuery = true)
    PageEntity findOneByPathAndSiteId(String path, int siteId);

    @Query(value = "SELECT * FROM search_engine.page p WHERE p.id > :startId", nativeQuery = true)
    List<PageEntity> findAllMoreThenStartId(@Param("startId") int startId);

    @Query(value = "SELECT * FROM search_engine.page p", nativeQuery = true)
    List<PageEntity> findAll();

    List<PageEntity> findAllByIdIn(List<Integer> pagesId);

    @Query(value = "SELECT COUNT(*) FROM search_engine.page p WHERE p.site_id = :siteId", nativeQuery = true)
    Integer getCountPagesOnSite(@Param("siteId") int siteId);

    @Query(value = "SELECT COUNT(*) FROM search_engine.page", nativeQuery = true)
    Integer getCountAllPages();

    @Query(value = "SELECT * FROM search_engine.page p " +
            "JOIN search_engine.index i ON i.page_id = p.id " +
            "WHERE i.lemma_id = ?1", nativeQuery = true)
    ArrayList<PageEntity> findByLemmaId(int lemmaId);

    @Query(value = "SELECT * FROM search_engine.page p " +
            "JOIN search_engine.index i ON i.page_id = p.id " +
            "JOIN search_engine.lemma l ON l.id = i.lemma_id " +
            "WHERE l.lemma = ?1", nativeQuery = true)
    ArrayList<PageEntity> findByLemma(String lemma);

    @Query(value = "SELECT p.* FROM search_engine.page p " +
            "JOIN search_engine.index i ON p.id = i.page_id " +
            "JOIN search_engine.lemma l ON i.lemma_id = l.id " +
            "WHERE l.lemma = :lemma AND l.site_id = :siteId", nativeQuery = true)
    ArrayList<PageEntity> findByLemmaAndSiteId(@Param("lemma") String lemma, @Param("siteId") Integer siteId);

}
