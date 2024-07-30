package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.entityes.SiteEntity;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    @Query(value = "SELECT s.id FROM search_engine.site s WHERE s.url = ?1", nativeQuery = true)
    Integer getIdByUrl(String url);

    @Query(value = "SELECT * FROM search_engine.site s WHERE s.url = ?1", nativeQuery = true)
    List<SiteEntity> findAllByUrl(String url);

    @Query(value = "SELECT s.status_indexing FROM search_engine.site s WHERE id = ?1", nativeQuery = true)
    String getStatusIndexing(int siteId);

    @Query(value = "SELECT s.last_error FROM search_engine.site s WHERE id = ?1", nativeQuery = true)
    String getErrorIndexing(int siteId);

    @Query(value = "SELECT s.status_time FROM search_engine.site s WHERE id = ?1", nativeQuery = true)
    LocalDateTime getStatusTimeIndexing(int siteId);

    @Override
    void deleteAll();
}
