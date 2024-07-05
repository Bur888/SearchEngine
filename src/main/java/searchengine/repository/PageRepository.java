package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.dto.entityesToDto.PageToDto;
import searchengine.model.entityes.PageEntity;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    @Query(value = "SELECT * FROM search_engine.page p WHERE p.path = ?1 and p.site_id = ?2", nativeQuery = true)
    PageEntity findOneByPathAndSiteId(String path, int siteId);

    @Query(value = "SELECT * FROM search_engine.page p WHERE p.id > :startId", nativeQuery = true)
    List<PageEntity> findAllMoreThenStartId(@Param("startId") int startId);

    @Query(value = "SELECT * FROM search_engine.page p", nativeQuery = true)
    List<PageEntity> findAll();

    @Query(value = "SELECT COUNT(*) FROM search_engine.page p WHERE p.site_id = :siteId", nativeQuery = true)
    Integer getCountPagesOnSite(@Param("siteId") int siteId);
}

