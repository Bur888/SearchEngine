package searchengine.model.findAndSaveLemmaAndIndex;

import lombok.Data;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

@Data
public class MultiLuceneMorphology {

    private RussianLuceneMorphology russianLuceneMorphology;
    private EnglishLuceneMorphology englishLuceneMorphology;

    public MultiLuceneMorphology(RussianLuceneMorphology russianLuceneMorphology,
                                 EnglishLuceneMorphology englishLuceneMorphology) {
        this.russianLuceneMorphology = russianLuceneMorphology;
        this.englishLuceneMorphology = englishLuceneMorphology;
    }
}
