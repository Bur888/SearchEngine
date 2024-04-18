package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.entityes.SiteEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    @Query(value = "SELECT * FROM search_engine.site s where s.url = ?1",
            nativeQuery = true)

           // "SELECT s FROM site s where s.url = ?1")
    public List<SiteEntity> getByUrl(String url);

}
