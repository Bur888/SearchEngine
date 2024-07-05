package searchengine.model.findAndSaveLemmaAndIndex;

import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import javax.print.Doc;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LemmaFinder {
    private final LuceneMorphology luceneMorphology; // = new RussianLuceneMorphology();
    private String WORD_TYPE_REGEX = "^[а-яА-ЯёЁ]+$";
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};

    public LemmaFinder(LuceneMorphology luceneMorphology) {
        this.luceneMorphology = luceneMorphology;
    }

    public HashMap<String, Integer> getLemmas(Document document) {
        String text = cleanDocument(document);
        HashMap<String, Integer> lemmas = new HashMap<>();
        String[] words = text.split("\\s+");
        if (words.length == 0) {
            return null;
        }
        for (String word : words) {
            if (!matchingTheTextToTheType(word)) {
                continue;
            }
            if (matchingParticlesNames(word)) {
                continue;
            }
            List<String> wordBaseForms =
                    luceneMorphology.getNormalForms(word);
            String wordBase = wordBaseForms.get(0);
            if (!lemmas.containsKey(wordBase)) {
                lemmas.put(wordBaseForms.get(0), 1);
            } else {
                lemmas.put(wordBase, lemmas.get(wordBase) + 1);
            }
        }
        return lemmas;
    }

    public boolean matchingTheTextToTheType(String text) {
        Pattern pattern = Pattern.compile(WORD_TYPE_REGEX);
        Matcher matcher = pattern.matcher(text);
        return matcher.matches();
    }

    public boolean matchingParticlesNames(String text) {
        List<String> wordBaseForms = luceneMorphology.getMorphInfo(text);
        boolean match = wordBaseForms.stream()
                .anyMatch((word) -> {
                    for (String particle : particlesNames) {
                        if (particle.contains(word)) {
                            return true;
                        }
                    }
                    return false;
                });
        return match;
    }

    public String cleanDocument(Document document) {
        Elements elements = document.select("*");
        for (Element element : elements) {
            element.textNodes().stream()
                    .filter(textNode -> textNode.getWholeText().trim().isEmpty())
                    .forEach(textNode -> textNode.remove());
        }
        return document.body().text().toLowerCase(Locale.ROOT);
    }
}
