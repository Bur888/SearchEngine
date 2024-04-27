package searchengine.model.entityes;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.nodes.Document;

import javax.persistence.*;

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

    @Column//(columnDefinition = "TEXT NOT NULL, Index(path(512))")
    private String path;

    @Column(nullable = false)
    private int code;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;
}
