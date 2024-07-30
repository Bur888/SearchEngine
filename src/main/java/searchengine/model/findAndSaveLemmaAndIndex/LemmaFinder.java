package searchengine.model.findAndSaveLemmaAndIndex;

import org.apache.logging.log4j.LogManager;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;


import java.util.*;

import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LemmaFinder {
    private LuceneMorphology luceneMorphology;
    private MultiLuceneMorphology multiLuceneMorphology;
    private static String WORD_TYPE_REGEX = "^[а-яёА-ЯЁa-zA-Z]+$";
    private static String RUS_WORD_TYPE_REGEX = "^[а-яёА-ЯЁ]+$";
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    private static List<String> stopWords = Arrays.asList("a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into", "is", "it", "no", "not", "of", "on", "or", "such", "that", "the", "their", "then", "there", "these", "they", "this", "to", "was", "will", "with");
    public static final Logger logger = LogManager.getLogger(MultiLuceneMorphology.class);

    public LemmaFinder(MultiLuceneMorphology multiLuceneMorphology) {
        this.multiLuceneMorphology = multiLuceneMorphology;
    }

    public HashMap<String, Integer> getLemmas(Document document) {
        String text = cleanDocument(document);
        HashMap<String, Integer> lemmas = new HashMap<>();
        String[] words = text.split("\\s+");
        if (words.length == 0) {
            return null;
        }

        for (String word : words) {
            try {
                if (!matchingTheTextToTheType(word)) {
                    continue;
                }

                if (isRussian(word)) {
                    luceneMorphology = multiLuceneMorphology.getRussianLuceneMorphology();
                    if (matchingRussianParticlesNames(word)) {
                        continue;
                    }
                } else {
                    luceneMorphology = multiLuceneMorphology.getEnglishLuceneMorphology();
                    if(matchingEnglishStopWords(word)) {
                        continue;
                    }
                }

                List<String> wordBaseForms =
                        luceneMorphology.getNormalForms(word);
                String wordBase = wordBaseForms.get(0);
                if (!lemmas.containsKey(wordBase)) {
                    lemmas.put(wordBaseForms.get(0), 1);
                } else {
                    lemmas.put(wordBase, lemmas.get(wordBase) + 1);
                }
            } catch (Exception exception) {
                logger.info("Слово " + word + " не проиндексировалось");
            }
        }
        return lemmas;
    }

    public boolean matchingTheTextToTheType(String text) {
        Pattern pattern = Pattern.compile(WORD_TYPE_REGEX);
        Matcher matcher = pattern.matcher(text);
        return matcher.matches();
    }

    public boolean matchingRussianParticlesNames(String text) {
        List<String> wordBaseForms = luceneMorphology.getMorphInfo(text);
        return wordBaseForms.stream()
                .anyMatch((word) -> {
                    for (String particle : particlesNames) {
                        if (word.contains(particle)) {
                            return true;
                        }
                    }
                    return false;
                });
    }

    public boolean matchingEnglishStopWords(String text) {
        return stopWords.stream().anyMatch(word -> Objects.equals(word, text));
    }

    public static String cleanDocument(Document document) {
        Elements elements = document.select("*");
        for (Element element : elements) {
            element.textNodes().stream()
                    .filter(textNode -> textNode.getWholeText().trim().isEmpty())
                    .forEach(Node::remove);
        }
        return document.body().text().toLowerCase(Locale.ROOT);
    }

    public static boolean isRussian(String text) {
        Pattern pattern = Pattern.compile(RUS_WORD_TYPE_REGEX);
        Matcher matcher = pattern.matcher(text);
        return matcher.matches();
    }
}
