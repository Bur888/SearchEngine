package searchengine.model.entityes;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "lemma")
public class LemmaEntity implements Comparable<LemmaEntity>{
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

    public int compareTo(LemmaEntity o2) {
        int frequencyComparison = Integer.compare(this.frequency, o2.getFrequency());
        if (frequencyComparison != 0) {
            return frequencyComparison;
        }
        int lemmaComparison = this.lemma.compareTo(o2.getLemma());
        if (lemmaComparison != 0) {
            return lemmaComparison;
        }
        return Integer.compare(this.siteId, o2.getSiteId());
    }

/*
    public int compareTo(LemmaEntity o2) {
        int siteIdComparison = Integer.compare(this.siteId, o2.getSiteId());
        if (siteIdComparison != 0) {
            return siteIdComparison;
        }
        return this.lemma.compareTo(o2.getLemma());
    }
*/
}
