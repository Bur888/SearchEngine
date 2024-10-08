package searchengine.model.findAndSaveLemmaAndIndex;

import lombok.Getter;
import lombok.Setter;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.model.entityes.IndexEntity;
import searchengine.model.entityes.LemmaEntity;
import searchengine.model.entityes.PageEntity;
import searchengine.services.IndexCRUDService;
import searchengine.services.IndexingService;
import searchengine.services.LemmaCRUDService;
import searchengine.services.PageCRUDService;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

@Component
@Getter
@Setter
public class FindAndSaveLemmaAndIndex {
    @Getter
    @Setter
    private static int NUM = 0;
    private static int START_ID = 0;
    private static int END_ID = 0;
    @Getter
    @Setter
    private static int finishSave = 0;
    private PageCRUDService pageCRUDService;
    private LemmaCRUDService lemmaCRUDService;
    private IndexCRUDService indexCRUDService;
    @Getter
    @Setter
    private static HashSet<LemmaEntity> lemmasExistingInDB = new HashSet<>();
    @Getter
    @Setter
    private static HashSet<LemmaEntity> lemmasNotExistingInDB = new HashSet<>();

    @Autowired
    public FindAndSaveLemmaAndIndex(PageCRUDService pageCRUDService,
                                    LemmaCRUDService lemmaCRUDService,
                                    IndexCRUDService indexCRUDService) {
        this.pageCRUDService = pageCRUDService;
        this.lemmaCRUDService = lemmaCRUDService;
        this.indexCRUDService = indexCRUDService;
    }

    public void run() {
        if (NUM == 0) {
            List<PageEntity> pages = pageCRUDService.findAll();
            START_ID = pages.get(0).getId();
            END_ID = pages.get(pages.size() - 1).getId();
            findAndSaveLemmaAndIndex(pages);
            START_ID = END_ID;
            NUM++;
        } else {
            List<PageEntity> pages = pageCRUDService.findAllMoreThenStartId(START_ID);
            if (pages.isEmpty()) {
                findAndSaveLemmaAndIndex(pages);;
            } else {
                END_ID = pages.get(pages.size() - 1).getId();
                findAndSaveLemmaAndIndex(pages);
                START_ID = END_ID;
            }
        }
    }

    public void findAndSaveLemmaAndIndex(List<PageEntity> pages) {
        try {
            for (PageEntity page : pages) {
                if (!IndexingService.isStartIndexingFlag()) {
                    NUM = 0;
                    break;
                }
                if (page.getCode() >= 400 && page.getCode() <= 599) {
                    continue;
                }
                Document document = Jsoup.parse(page.getContent());
                MultiLuceneMorphology multiLuceneMorphology
                        = new MultiLuceneMorphology(new RussianLuceneMorphology(), new EnglishLuceneMorphology());
                LemmaFinder lemmaFinder = new LemmaFinder(multiLuceneMorphology);
                HashMap<String, Integer> lemmas = lemmaFinder.getLemmas(document);
                LemmaEntity lemmaForHashSet;
                int countWordOnPage = IndexEntity.getCountWordsOnPage(lemmas);
                float rank;
                boolean isLemmaInLemmasExistingInDB;
                boolean isLemmaInLemmasNotExistingInDB;


                for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                    lemmaForHashSet = new LemmaEntity();
                    lemmaForHashSet.setLemma(entry.getKey());
                    lemmaForHashSet.setSiteId(page.getSite().getId());


                    isLemmaInLemmasExistingInDB = lemmasExistingInDB.contains(lemmaForHashSet);
                    isLemmaInLemmasNotExistingInDB = lemmasNotExistingInDB.contains(lemmaForHashSet);

                    if (isLemmaInLemmasExistingInDB) {
                        for (LemmaEntity lemmaIterator : lemmasExistingInDB) {
                            if (Objects.equals(lemmaIterator, lemmaForHashSet)) {
                                lemmaIterator.setFrequency(lemmaIterator.getFrequency() + 1);
                            }
                        }
                    }

                    if (isLemmaInLemmasNotExistingInDB) {
                        for (LemmaEntity lemmaIterator : lemmasNotExistingInDB) {
                            if (Objects.equals(lemmaIterator, lemmaForHashSet)) {
                                lemmaIterator.setFrequency(lemmaIterator.getFrequency() + 1);
                            }
                        }
                    }

                    if (!isLemmaInLemmasNotExistingInDB && !isLemmaInLemmasExistingInDB) {
                        LemmaEntity lemmaFromDB = lemmaCRUDService.findByLemmaAndSiteId(entry.getKey(), page.getSite().getId());
                        if (lemmaFromDB == null) {
                            lemmaForHashSet.setFrequency(1);
                            lemmasNotExistingInDB.add(lemmaForHashSet);
                        } else {
                            lemmaForHashSet = lemmaFromDB;
                            lemmaForHashSet.setFrequency(lemmaForHashSet.getFrequency() + 1);
                            lemmasExistingInDB.add(lemmaForHashSet);
                        }
                    }

                    rank = IndexEntity.calculateRankValue(entry.getValue(), countWordOnPage);
                    IndexEntity index = new IndexEntity();
                    index.setPage(page);
                    index.setLemma(lemmaForHashSet);
                    index.setRank(rank);
                    IndexEntity.getIndexes().add(index);

                    if (IndexEntity.getIndexes().size() < 500) {
                        continue;
                    }
                    saveLemmaAndIndex();
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(String.valueOf(FindAndSaveLemmaAndIndex.class), "Ошибка в классе " + FindAndSaveLemmaAndIndex.class);
            throw new RuntimeException(ex);
        }
    }

    public synchronized void saveLemmaAndIndex() {
        lemmasNotExistingInDB = lemmaCRUDService.saveAll(lemmasNotExistingInDB);
        lemmasExistingInDB = lemmaCRUDService.updateAll(lemmasExistingInDB);

        HashSet<LemmaEntity> allLemmas = new HashSet<>();
        allLemmas.addAll(lemmasExistingInDB);
        allLemmas.addAll(lemmasNotExistingInDB);
        HashSet<IndexEntity> updateIndexes = new HashSet<>();

        for (IndexEntity indexEntity : IndexEntity.getIndexes()) {
            LemmaEntity lemma = allLemmas.stream()
                    .filter(l -> Objects.equals(l, indexEntity.getLemma()))
                    .findFirst().orElse(null);
            if (lemma != null) {
                indexEntity.setLemma(lemma);
                updateIndexes.add(indexEntity);
            }
        }
        indexCRUDService.saveAll(updateIndexes);
        lemmasExistingInDB.clear();
        lemmasNotExistingInDB.clear();
        IndexEntity.getIndexes().clear();
    }
}

