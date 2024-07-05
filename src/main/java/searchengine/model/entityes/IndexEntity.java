package searchengine.model.entityes;

//import searchengine.model.findAndSaveLemmaAndIndex.LemmaFinder;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

@Entity
@Getter
@Setter
@Table(name = "`index`")
public class IndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "page_id", nullable = false)
    private PageEntity page;

    @ManyToOne
    @JoinColumn(name = "lemma_id", nullable = false)
    private LemmaEntity lemma;

    @Column(name = "`rank`", nullable = false)
    private float rank;

    @Getter
    @Setter
    private static HashSet<IndexEntity> indexes = new HashSet<>();

    @Setter
    @Getter
    private static HashMap<LemmaEntity, Integer> lemmasAndPageId = new HashMap<>();



/*
    public static IndexEntity makeIndexEntity(LemmaEntity lemma,
                                         PageEntity pageEntity,
                                         Integer countThisWord,
                                         Integer countWordOnPage) {
        IndexEntity index = new IndexEntity();
        index.setLemma(lemma);
        index.setPage(pageEntity);
        float rank = calculateRankValue(countThisWord, countWordOnPage);
        index.setRank(rank);
        return index;
    }
*/

    public static int getCountWordsOnPage(HashMap<String, Integer> lemmas) {
        int countWords = 0;
        for (Integer value : lemmas.values()) {
            countWords += value;
        }
        return countWords;
    }

    public static float calculateRankValue(Integer countThisWord, Integer countWordOnPage) {
        return (float) countThisWord / countWordOnPage * 100;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexEntity index = (IndexEntity) o;
        return Objects.equals(page.getId(), index.getPage().getId()) && Objects.equals(lemma.getLemma(), index.getLemma().getLemma());
    }

    @Override
    public int hashCode() {
        int result = lemma == null ? 0 : lemma.getLemma().hashCode();
        return page != null ? (31 * result + page.getId()) : 31 * result;
/*
        if (page != null) {
            result = 31 * result + page.getId();
        } else {
            result = 31 * result;
        }
        return result;
*/
    }
}
