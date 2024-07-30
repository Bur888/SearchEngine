package searchengine.model.entityes;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "page")
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;

    @Column(columnDefinition = "TEXT NOT NULL, Index(path(512))")
    private String path;

    @Column(nullable = false)
    private int code;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL)
    private List<IndexEntity> indexes;

    public static PageEntity makePageEntityForSave(SiteEntity site, String url, String document, Integer code) {
        PageEntity pageEntity = new PageEntity();
        pageEntity.setSite(site);
        pageEntity.setPath(url);
        pageEntity.setContent(String.valueOf(document));
        pageEntity.setCode(code);
        return pageEntity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageEntity page = (PageEntity) o;
        return Objects.equals(site.getId(), page.site.getId()) && Objects.equals(path, page.getPath());
    }

    @Override
    public int hashCode() {
        int result = path == null ? 0 : path.hashCode();
        result = 31 * result + site.getId();
        return result;
    }
}
