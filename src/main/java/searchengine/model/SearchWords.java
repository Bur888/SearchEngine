package searchengine.model;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.WrongCharaterException;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.dto.searchResponse.Data;
import searchengine.dto.searchResponse.SearchResponse;
import searchengine.dto.searchResponse.SearchResponseFalse;
import searchengine.dto.searchResponse.SearchResponseTrue;
import searchengine.model.entityes.IndexEntity;
import searchengine.model.entityes.LemmaEntity;
import searchengine.model.entityes.PageEntity;
import searchengine.model.entityes.SiteEntity;
import searchengine.model.findAndSaveLemmaAndIndex.LemmaFinder;
import searchengine.model.findAndSaveLemmaAndIndex.MultiLuceneMorphology;
import searchengine.services.*;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SearchWords {

    private final LemmaCRUDService lemmaCRUDService;
    private final PageCRUDService pageCRUDService;
    private final IndexCRUDService indexCRUDService;
    private final SiteCRUDService siteCRUDService;
    private static double maxPagesPercent = 0.70;
    private LuceneMorphology luceneMorphology;
    private final ArrayList<String> deleteLemmaFromQuery = new ArrayList<>();

    @Autowired
    public SearchWords(LemmaCRUDService lemmaCRUDService,
                       PageCRUDService pageCRUDService,
                       IndexCRUDService indexCRUDService,
                       SiteCRUDService siteCRUDService) {
        this.lemmaCRUDService = lemmaCRUDService;
        this.pageCRUDService = pageCRUDService;
        this.indexCRUDService = indexCRUDService;
        this.siteCRUDService = siteCRUDService;
    }

    public SearchResponse search(String query, String site, Integer offset, Integer limit) {
        try {
            Document queryDoc = Jsoup.parse("");
            queryDoc.body().text(query);
            MultiLuceneMorphology multiLuceneMorphology = new MultiLuceneMorphology(new RussianLuceneMorphology(),
                    new EnglishLuceneMorphology());
            LemmaFinder lemmaFinder = new LemmaFinder(multiLuceneMorphology);
            HashMap<String, Integer> lemmas = lemmaFinder.getLemmas(queryDoc);

            if (lemmas.isEmpty()) {
                return new SearchResponseFalse("Запрос задан некорректно, ничего не найдено");
            }

            Map<LemmaEntity, Integer> lemmasFromDB;

            if (site != null) {
                lemmasFromDB = findOnOneSite(lemmas, site);
            } else {
                lemmasFromDB = findOnAllSites(lemmas);
            }
            if (lemmasFromDB.containsKey(null)) {
                return new SearchResponseFalse("В запросе имеются слова, которые отсутствуют на интересующих сайтах");
            }

            int countPages = site != null ? pageCRUDService.getCountPagesOnSite(site) : pageCRUDService.getCountAllPages();
            //удаляем часто встречающиеся леммы
            lemmasFromDB = removeFrequentlyOccurringLemmas(lemmasFromDB, countPages);

            //создаем TreeMap и прописываем условия сортировки
            Map<LemmaEntity, Integer> finalLemmasFromDB1 = lemmasFromDB;
            Map<LemmaEntity, Integer> sortLemmas = new TreeMap<>(Comparator
                    .comparingInt(LemmaEntity::getFrequency)
                    .thenComparing(LemmaEntity::getLemma)
                    .thenComparingInt(LemmaEntity::getSiteId));
            sortLemmas.putAll(lemmasFromDB);

            List<Integer> pagesIdForFirstLemma = new ArrayList<>();
            int num = 0;
            String lastLemma = "";
            for (Map.Entry<LemmaEntity, Integer> lemmaEntityIntegerEntry : sortLemmas.entrySet()) {
                LemmaEntity lemma = lemmaEntityIntegerEntry.getKey();
                if (num == 0) {
                    if (site == null) {
                        pagesIdForFirstLemma = pageCRUDService.findByLemma(lemma.getLemma())
                                .stream()
                                .map(PageEntity::getId)
                                .limit(100)
                                .toList();
                    } else {
                        pagesIdForFirstLemma = pageCRUDService.findByLemmaAndSiteId(lemma.getLemma(), lemma.getSiteId())
                                .stream()
                                .map(PageEntity::getId)
                                .limit(100)
                                .toList();
                    }
                    num++;
                    lastLemma = lemma.getLemma();
                    continue;
                }
                if (Objects.equals(lemma.getLemma(), lastLemma)) {
                    continue;
                }

                List<Integer> pagesIdForNextLemma = pageCRUDService.findByLemma(lemma.getLemma()).stream()
                        .map(PageEntity::getId)
                        .toList();
                List<Integer> modifiablePagesIdForFirstLemma = new ArrayList<>(pagesIdForFirstLemma);
                modifiablePagesIdForFirstLemma.removeIf(pageId -> !pagesIdForNextLemma.contains(pageId));
                pagesIdForFirstLemma = modifiablePagesIdForFirstLemma;
                lastLemma = lemma.getLemma();
            }

            if (pagesIdForFirstLemma.isEmpty()) {
                return new SearchResponseFalse("Наличие всех слов запроса на какой либо странице " +
                        "интересующих сайтов не найдено");
            }

            List<PageEntity> pages = pageCRUDService.findAllByIdIn(pagesIdForFirstLemma);
            if (pages.isEmpty()) {
                SearchResponseTrue responseTrue = new SearchResponseTrue();
                responseTrue.setCount(0);
                responseTrue.setData(null);
                return responseTrue;
            }

            //считаем абсолютную релевантность к каждой странице
            Map<PageEntity, Float> pageAndAbsRelevance;

            Map<LemmaEntity, Integer> finalLemmasFromDB2 = lemmasFromDB;
            pageAndAbsRelevance = pages.stream().collect(Collectors.toMap(
                    Function.identity(),
                    page -> (float) finalLemmasFromDB2.keySet().stream()
                            .mapToDouble(lemma -> {
                                IndexEntity index = indexCRUDService.findByPageIdAndLemmaId(page.getId(), lemma.getId());
                                return index != null ? index.getRank() : 0;
                            })
                            .sum(),
                    Math::max)
            );

            float maxAbsRelevance = pageAndAbsRelevance.values().stream().max(Float::compare).get();

            Map<PageEntity, Float> pageAndRelevance = pageAndAbsRelevance.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue() / maxAbsRelevance
                    ));
            //сортируем по величине относительной релевантности
            Map<PageEntity, Float> sortedPageAndRelevance = pageAndRelevance.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

            ArrayList<String> queryBaseFormWords = new ArrayList<>(lemmas.keySet());

            List<Data> datas = sortedPageAndRelevance.entrySet().stream()
                    .map(entry -> {
                        Data data = new Data();
                        SiteEntity newSite = siteCRUDService.getById(entry.getKey().getSite().getId());
                        String html = entry.getKey().getContent();
                        Document doc = Jsoup.parse(html);
                        Element element = doc.select("head").first();
                        if (element != null) {
                            Element title = element.select("title").first();
                            if (title != null) {
                                String textTitle = title.text();
                                data.setTitle(textTitle);
                            }
                        }
                        data.setRelevance(entry.getValue());
                        data.setUri(entry.getKey().getPath());
                        data.setSite(newSite.getUrl());
                        data.setSiteName(newSite.getName());
                        data.setSnippet(getSnippet(queryBaseFormWords, doc, multiLuceneMorphology));
                        return data;
                    })
                    .collect(Collectors.toList());
            SearchResponseTrue searchResponseTrue = new SearchResponseTrue();
            if (offset + limit < datas.size()) {
                searchResponseTrue.setData(datas.subList(offset, offset + limit));
            } else {
                searchResponseTrue.setData(datas.subList(offset, datas.size()));
            }
            searchResponseTrue.setCount(datas.size());
            return searchResponseTrue;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<LemmaEntity, Integer> findOnAllSites(HashMap<String, Integer> lemmas) {
        Map<LemmaEntity, Integer> lemmasFromDB = new HashMap<>();
        List<String> lemmasList = new ArrayList<>(lemmas.keySet());
        List<LemmaEntity> lemmasFromDBList = lemmaCRUDService.findAllByLemmas(lemmasList);
        for (LemmaEntity lemma : lemmasFromDBList) {
            if (lemma != null) {
                lemmasFromDB.put(lemma, lemma.getFrequency());
            } else {
                lemmasFromDB.put(null, 0);
            }
        }
        return lemmasFromDB;
    }

    public Map<LemmaEntity, Integer> findOnOneSite(HashMap<String, Integer> lemmas, String site) {
        Map<LemmaEntity, Integer> lemmasFromDB = lemmas.keySet().stream()
                .map(lemma -> Optional.ofNullable(lemmaCRUDService.findByLemmaAndUrl(lemma, site)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(lemmaEntity -> lemmaEntity, LemmaEntity::getFrequency));

        if (lemmasFromDB.size() != lemmas.size()) {
            lemmasFromDB.put(null, 0);
        }
        return lemmasFromDB;
    }

    public Map<LemmaEntity, Integer> removeFrequentlyOccurringLemmas(Map<LemmaEntity, Integer> lemmas, int countPages) {
        Map<LemmaEntity, Integer> originalLemmas = new HashMap<>(lemmas);
        Map<String, Integer> lemmasAndFrequency = lemmas.keySet().stream()
                .collect(Collectors.groupingBy(
                        LemmaEntity::getLemma,
                        Collectors.summingInt(LemmaEntity::getFrequency))
                );

        for (Map.Entry<String, Integer> entry : lemmasAndFrequency.entrySet()) {
            Integer frequency = entry.getValue();
            String lemma = entry.getKey();
            if ((double) frequency / countPages > maxPagesPercent) {
                deleteLemmaFromQuery.add(lemma);
                lemmas.keySet().removeIf(lemmaEntity -> Objects.equals(lemmaEntity.getLemma(), lemma));
            }
        }
        lemmas.keySet().stream()
                .collect(Collectors.groupingBy(
                        LemmaEntity::getLemma,
                        Collectors.summingInt(LemmaEntity::getFrequency))
                );
        return lemmas.size() < 2 ? originalLemmas : lemmas;
    }

    public String getSnippet(ArrayList<String> queryBaseFormWords, Document document, MultiLuceneMorphology multiLuceneMorphology) {
        String text = LemmaFinder.cleanDocument(document);
        String[] words = text.split("\\s+");
        String[] updateWords = cleanWords(words);
        ArrayList<String> normalFormsWords = new ArrayList<>();

        //привожу все слова текста к нормальным формам
        for (String word : updateWords) {
            if (LemmaFinder.isRussian(word)) {
                luceneMorphology = multiLuceneMorphology.getRussianLuceneMorphology();
            } else {
                luceneMorphology = multiLuceneMorphology.getEnglishLuceneMorphology();
            }
            try {
                List<String> wordBaseForm = luceneMorphology.getNormalForms(word);
                normalFormsWords.add(wordBaseForm.get(0));
            } catch (WrongCharaterException | ArrayIndexOutOfBoundsException exception) {
                //Logger logger = Logger.getLogger(SearchService.class.getName());
                //logger.log(Level.WARNING, "Непонятное слово - " + word);
                normalFormsWords.add(word);
            }
        }

        int maxWords = 10;
        int maxChars = 200;
        int minContextWords = 2;

        ArrayList<Integer> matches = new ArrayList<>();
        ArrayList<String> queryBaseFormWordsCopy = new ArrayList<>(queryBaseFormWords);

        for (int i = 0; i < normalFormsWords.size(); i++) {
            if (queryBaseFormWordsCopy.contains(normalFormsWords.get(i))) {
                matches.add(i);
                queryBaseFormWordsCopy.remove(normalFormsWords.get(i));
                if (queryBaseFormWordsCopy.isEmpty()) {
                    break;
                }
            }
        }

        StringBuilder snippet = new StringBuilder();
        for (int match : matches) {
            if (snippet.length() > maxChars) {
                break;
            }
            int beginSnippet = Math.max(match - minContextWords, 0);
            int finishSnippet = Math.min(match + maxWords, normalFormsWords.size() - 1);

            if (beginSnippet > 1) {
                snippet.append("...");
            }
            for (int i = beginSnippet; i <= finishSnippet; i++) {

                if (i == match) {
                    snippet.append("<b>").append(words[i]).append("</b>");
                } else {
                    snippet.append(words[i]);
                }
                if (i < finishSnippet) {
                    snippet.append(" ");
                }
                if (snippet.length() > maxChars) {
                    break;
                }
            }
            if (finishSnippet < words.length - 1) {
                snippet.append("...").append("<br>").append(" ");
            }
        }
        return snippet.toString();
    }

    public String[] cleanWords(String[] words) {
        String[] cleanWords = new String[words.length];
        for (int i = 0; i < words.length; i++) {
            cleanWords[i] = words[i].replaceAll("[.,!?:;)(~`'\"]", "");
        }
        return cleanWords;
    }
}
