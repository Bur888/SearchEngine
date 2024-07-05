package searchengine.config;

import org.springframework.stereotype.Component;
import searchengine.model.entityes.LemmaEntity;
import org.springframework.core.convert.converter.Converter;

@Component
public class LemmaConverter implements Converter<Object[], LemmaEntity> {
    @Override
    public LemmaEntity convert(Object[] source) {
        if (source.length != 4) {
            throw new IllegalArgumentException("Source array must contain 4 elements");
        }
        LemmaEntity lemma = new LemmaEntity();
        lemma.setId((Integer) source[0]);
        lemma.setFrequency((Integer) source[1]);
        lemma.setLemma((String) source[2]);
        lemma.setSiteId((Integer) source[3]);
        return lemma;
    }
}
