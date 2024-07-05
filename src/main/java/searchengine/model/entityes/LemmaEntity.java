package searchengine.model.entityes;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "lemma")
public class LemmaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "site_id", nullable = false)
    private int siteId;

    @Column(nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL)
    private List<IndexEntity> indexes;

    @Setter
    @Getter
    private static HashSet<LemmaEntity> lemmasHashSet = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        LemmaEntity lemmaEntity = (LemmaEntity) o;
        return Objects.equals(this.siteId, lemmaEntity.getSiteId()) && Objects.equals(this.lemma, lemmaEntity.getLemma());
    }

    @Override
    public int hashCode() {
        int result = lemma == null ? 0 : lemma.hashCode();
        result = 31 * result + siteId;
        return result;
    }
}
