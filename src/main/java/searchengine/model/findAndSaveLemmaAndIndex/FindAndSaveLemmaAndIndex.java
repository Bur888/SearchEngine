package searchengine.model.findAndSaveLemmaAndIndex;

import lombok.Getter;
import lombok.Setter;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.model.entityes.IndexEntity;
import searchengine.model.entityes.LemmaEntity;
import searchengine.model.entityes.PageEntity;
import searchengine.services.IndexCRUDService;
import searchengine.services.LemmaCRUDService;
import searchengine.services.PageCRUDService;

import javax.persistence.criteria.CriteriaBuilder;
import java.io.IOException;
import java.util.*;

@Component
@Getter
@Setter
public class FindAndSaveLemmaAndIndex {
    @Getter
    @Setter
    //private static boolean START_INDEXING_PAGES;
    private static int NUM = 0;
    private static int START_ID = 0;
    private static int END_ID = 0;
    @Getter
    @Setter
    private static int finishSave = 0;
    private PageCRUDService pageCRUDService;
    private LemmaCRUDService lemmaCRUDService;
    private IndexCRUDService indexCRUDService;
    private static ArrayList<LemmaEntity> lemmasForDB = new ArrayList<>();
    private static HashMap<LemmaEntity, Integer> lemmasForIndexes = new HashMap<>();
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
            END_ID = pages.get(pages.size() - 1).getId();
            findAndSaveLemmaAndIndex(pages);
            START_ID = END_ID;
        }
    }

    public void findAndSaveLemmaAndIndex(List<PageEntity> pages) {
        try {
            // finishSave = false;
            for (PageEntity page : pages) {
                if(page.getCode() >= 400 && page.getCode() <= 599) {
                    continue;
                }
                Document document = Jsoup.parse(page.getContent());
                LemmaFinder lemmaFinder = new LemmaFinder(new RussianLuceneMorphology());
                HashMap<String, Integer> lemmas = lemmaFinder.getLemmas(document);
                LemmaEntity lemmaForHashSet;
                int countWordOnPage = IndexEntity.getCountWordsOnPage(lemmas);
                float rank;
                boolean lemmaInLemmasExistingInDB;
                boolean lemmaInLemmasNotExistingInDB;


                for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                    lemmaForHashSet = new LemmaEntity();
                    lemmaForHashSet.setLemma(entry.getKey());
                    lemmaForHashSet.setSiteId(page.getSite().getId());


                    lemmaInLemmasExistingInDB = lemmasExistingInDB.contains(lemmaForHashSet);
                    lemmaInLemmasNotExistingInDB = lemmasNotExistingInDB.contains(lemmaForHashSet);

                    if (lemmaInLemmasExistingInDB) {
                        LemmaEntity finalLemmaForHashSet = lemmaForHashSet;
                        lemmasExistingInDB.stream()
                                .filter(lemma -> Objects.equals(lemma, finalLemmaForHashSet))
                                .forEach(lemma -> lemma.setFrequency(lemma.getFrequency() + 1));
                    }
                    if (lemmaInLemmasNotExistingInDB) {
                        LemmaEntity finalLemmaForHashSet = lemmaForHashSet;
                        lemmasNotExistingInDB.stream()
                                .filter(lemma -> Objects.equals(lemma, finalLemmaForHashSet))
                                .forEach(lemma -> lemma.setFrequency(lemma.getFrequency() + 1));
                    }
                    if (!lemmaInLemmasNotExistingInDB && !lemmaInLemmasExistingInDB) {
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

            // finishSave = true;
/*
            if (finishSave) {
                saveLemmaAndIndex();
            }
*/
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void saveLemmaAndIndex() {
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


/*

                    //TODO прописать запрос к БД есть ли лемма по SiteId и лемме, присвоить этот объект Индексу и потом перебирая, сохранять индексы
                    lemmaForHashSet = lemmaCRUDService.findByLemmaAndSiteId(entry.getKey(), page.getSite().getId());
                    if (lemmaForHashSet != null) {
                        lemmaForHashSet.setFrequency(lemmaForHashSet.getFrequency() + 1);
                        lemmasExistingInDB.add(lemmaForHashSet);
                    } else {
                        lemmaForHashSet = new LemmaEntity();
                        lemmaForHashSet.setLemma(entry.getKey());
                        lemmaForHashSet.setSiteId(page.getSite().getId());
                        lemmaForHashSet.setFrequency(1);
                        lemmasNotExistingInDB.add(lemmaForHashSet);
                    }
*/

/*
                    lemmaForHashSet = new LemmaEntity();
                    lemmaForHashSet.setLemma(entry.getKey());
                    lemmaForHashSet.setSiteId(page.getSite().getId());
                    lemmaForHashSet.setFrequency(1);
                    lemmasForIndexes.merge(lemmaForHashSet, 1, (oldValue, newValue) -> oldValue + newValue);

*/
/*
                    rank = IndexEntity.calculateRankValue(entry.getValue(), countWordOnPage);
                    IndexEntity index = new IndexEntity();
                    index.setPage(page);
                    index.setLemma(lemmaForHashSet);
                    index.setRank(rank);
                    IndexEntity.getIndexes().add(index);
                }
                lemmaCRUDService.saveAll(lemmasNotExistingInDB);
                lemmaCRUDService.updateAll(lemmasExistingInDB);
*/
/*

                    if (IndexEntity.getIndexes().size() < 3) {
                        continue;
                    }
*/

/*

                    for (Map.Entry<LemmaEntity, Integer> lemma : lemmasForIndexes.entrySet()) {
                        LemmaEntity lemmaEntity = lemma.getKey();
                        if (lemma.getValue() != 1) {
                            lemmaEntity.setFrequency(lemma.getValue());
                        }
                        lemmasForDB.add(lemmaEntity);
                    }

                    ArrayList<LemmaEntity> updateLemmas = new ArrayList<>();
                    for (LemmaEntity lemma : lemmas) {
                        LemmaEntity lemmaEntity = lemmaCRUDService.findByLemmaAndSiteId(lemma.getLemma(), lemma.getSiteId());
                        if (lemmaEntity == null) {
                            updateLemmas.add(lemma);
                        } else {
                            lemmaEntity.setFrequency(lemmaEntity.getFrequency() + lemma.getFrequency());
                            updateLemmas.add(lemmaEntity);
                        }
                    }

                    lemmaCRUDService.updateAll(lemmasForDB);

                    for (IndexEntity indexEntity : IndexEntity.getIndexes()) {
                        LemmaEntity lemma = lemmaCRUDService.findByLemmaAndSiteId(indexEntity.getLemma().getLemma(),
                                indexEntity.getLemma().getSiteId());
                        if (lemma == null) {
                            lemmaCRUDService.save(indexEntity.getLemma());
                        } else {
                            int newFrequency = lemma.getFrequency() + indexEntity.getLemma().getFrequency();
                            lemma.setFrequency(newFrequency);
                            indexEntity.setLemma(lemma);
                        }
                        indexCRUDService.save(indexEntity);
                    }
                    IndexEntity.getIndexes().clear();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
//TODO здесь подумать как сделать мультисохранение в БД лемм и затем выполнить сохранение в БД индексов.
// //TODO выполнить проверку существует ли лема в БД перед сохранением?

*/
/*
                if(IndexEntity.getLemmasAndPageId().size() < 2) {
                    continue;
                }

                for (Map.Entry<LemmaEntity, PageEntity> entry : IndexEntity.getLemmasAndPageId().entrySet()) {
                    LemmaEntity lemmaForDB;
                    lemmaForDB = lemmaCRUDService.findByLemmaAndSiteId(entry.getKey().getLemma(),
                                                                        entry.getKey().getSiteId());
                    if (lemmaForDB == null) {
                        lemmaCRUDService.save(entry.getKey());
                    }
                    indexCRUDService.makeIndexEntity(entry.getKey(), entry.getValue(), )

                }
*/
/*

                int newFrequency = 0;
                for (LemmaEntity lemma : IndexEntity.getLemmasAndPages().keySet()) {
                    LemmaEntity lemmaForDB;
                    lemmaForDB = lemmaCRUDService.findByLemmaAndSiteId(lemma.getLemma(), lemma.getSiteId());
                    if (lemmaForDB != null) {
                        newFrequency = lemmaForDB.getFrequency() + lemma.getFrequency();
                        lemmaForDB.setFrequency(newFrequency);
                        lemmasExistingInDB.add(lemmaForDB);
                    } else {
                        lemmasNotExistingInDB.add(lemma);
                    }
                }
*/
//lemmaCRUDService.updateAll(lemmasExistingInDB);
//  lemmasExistingInDB.clear();

// lemmaCRUDService.saveAll(lemmasNotExistingInDB);
//lemmasNotExistingInDB.clear();


//TODO сохранить все леммы в БД, а затем делать индексы,их сохранять и все списки очистить


/*

                    int newFrequency = LemmaEntity.getLemmasHashSet()
                            .stream()
                            .filter(el -> el.equals(finalLemma))
                            .map(el -> el.getFrequency() + 1)
                            .findFirst()
                            .orElse(1);
                    LemmaEntity.getLemmasHashSet().remove(lemmaForHashMap);
                    lemmaForHashMap.setFrequency(newFrequency);
                    LemmaEntity.getLemmasHashSet().add(lemmaForHashMap);
*/

/*
                IndexEntity index = new IndexEntity();
                index = indexCRUDService.makeIndexEntity(lemmaForHashSet, page, entry.getValue(), countWordOnPage);
                IndexEntity.getIndexes().add(index);

                if (LemmaEntity.getLemmasHashSet().size() < 2) {
                    continue;
                }
                //LemmaEntity lemmaForDB;
                for (LemmaEntity lemma : LemmaEntity.getLemmasHashSet()) {
                    LemmaEntity lemmaForDB;
                    lemmaForDB = lemmaCRUDService.findByLemmaAndSiteId(lemma.getLemma(), lemma.getSiteId());
                    if (lemmaForDB != null) {
                        newFrequency = lemmaForDB.getFrequency() + lemma.getFrequency();
                        lemmaForDB.setFrequency(newFrequency);
                        lemmasExistingInDB.add(lemmaForDB);
                    } else {
                        lemmasNotExistingInDB.add(lemma);
                    }
                }
                saveAndUpdateLemmaAndIndex();
            }
            saveAndUpdateLemmaAndIndex();
            LemmaEntity.getLemmasHashSet().clear();
        }
    } catch(
    IOException ex)

    {
        throw new RuntimeException(ex);
    }
*/
/*

    public void saveAndUpdateLemmaAndIndex() {

        lemmaCRUDService.updateAll(lemmasExistingInDB);
        lemmasExistingInDB.clear();

        lemmaCRUDService.saveAll(lemmasNotExistingInDB);
        lemmasNotExistingInDB.clear();

        indexCRUDService.saveAll(IndexEntity.getIndexes());
        IndexEntity.getIndexes().clear();
    }
}

*/