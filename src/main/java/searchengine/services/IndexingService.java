package searchengine.services;

import lombok.Getter;
import lombok.Setter;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.entityesToDto.PageToDto;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.IndexingResponseFalse;
import searchengine.dto.indexing.IndexingResponseTrue;
//import searchengine.model.findAndSaveLemmaAndIndex.LemmaFinder;
import searchengine.model.StartIndexingSites;
import searchengine.model.entityes.*;
import searchengine.model.findAndSaveLemmaAndIndex.FindAndSaveLemmaAndIndex;
import searchengine.model.findAndSaveLemmaAndIndex.LemmaFinder;
import searchengine.model.searchLinks.*;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class IndexingService {

    @Getter
    @Setter
    private static boolean startIndexingFlag;
    @Autowired
    private SiteCRUDService siteCRUDService;
    @Autowired
    private PageCRUDService pageCRUDService;
    @Autowired
    private LemmaCRUDService lemmaCRUDService;
    @Autowired
    private IndexCRUDService indexCRUDService;
    @Autowired
    private SitesList sitesList;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private ThreadForSavePageAndSiteInDB savePageAndSiteInDB;
    private Thread startIndexing;

    public IndexingResponse startIndexing() {

        if (startIndexingFlag == true) {
            return new IndexingResponseFalse("Индексация уже запущена");
        }
        ParseWebRecursive.setStopNow(false);
        startIndexingFlag = true;
        startIndexing = new Thread(new StartIndexingSites(siteCRUDService,
                pageCRUDService,
                lemmaCRUDService,
                indexCRUDService,
                jdbcTemplate,
                sitesList));
        startIndexing.start();
        return new IndexingResponseTrue();
    }

    public IndexingResponse stopIndexing() {
        if (startIndexingFlag == false) {
            return new IndexingResponseFalse("Индексация не запущена");
        }
        ParseWebRecursive.setStopNow(true);
        for (Thread thread : StartIndexingSites.getThreads()) {
            thread.interrupt();
        }

        startIndexing.interrupt();
        PageToDto.getPageToDtoHashMap().clear();
        Link.getAllLinks().clear();
        startIndexingFlag = false;
        return new IndexingResponseTrue();
    }

    public IndexingResponse indexPage(String url) {
        try {
            String rootUrl = "";
            String nameSite = "";
            for (Site site : sitesList.getSites()) {
                Link.getRootUrls().add(site.getUrl());
                Pattern pattern = Pattern.compile(site.getUrl());
                Matcher matcher = pattern.matcher(url);
                if (matcher.find()) {
                    rootUrl = site.getUrl();
                    nameSite = site.getName();
                }
            }
            if (rootUrl.isEmpty()) {
                return new IndexingResponseFalse("Данная страница находится за пределами сайтов " +
                        "указанных в конфигурационном файле");
            }
            ConnectionWeb connection = new ConnectionWeb();
            Document document = connection.getDocument(url);
            SiteEntity siteEntity = siteCRUDService.getByUrl(rootUrl);
            Integer siteId;
            if (siteEntity == null) {
                siteEntity = new SiteEntity();
                siteEntity.setStatusIndexing(StatusIndexing.INDEXING);
                siteEntity.setUrl(rootUrl);
                siteEntity.setName(nameSite);
                siteCRUDService.save(siteEntity);
                siteId = siteCRUDService.getIdByUrl(rootUrl);
            } else {
                siteId = siteEntity.getId();
            }
            String updateUrl = Link.clearUrl(url);
            updateUrl = Link.urlWithoutRoot(updateUrl);
            PageEntity pageEntity = pageCRUDService.findOneByPathAndSiteId(updateUrl, siteId);
            if (pageEntity == null) {
                pageEntity = PageEntity.makePageEntityForSave(siteEntity, updateUrl, document.html(), 200);
            } else {
                List<LemmaEntity> lemmasList = lemmaCRUDService.findLemmasByPageId(pageEntity.getId());
                Iterator<LemmaEntity> iterator = lemmasList.iterator();
                while (iterator.hasNext()) {
                    LemmaEntity lemma = iterator.next();
                    lemma.setFrequency(lemma.getFrequency() - 1);
                    if (lemma.getFrequency() == 0) {
                        lemmaCRUDService.delete(lemma);
                        iterator.remove();
                    }
                }
                lemmaCRUDService.updateAll(new HashSet<>(lemmasList));
            }
            pageCRUDService.delete(pageEntity);

            pageEntity.setContent(document.html());
            pageCRUDService.save(pageEntity);
            pageEntity = pageCRUDService.findOneByPathAndSiteId(updateUrl, siteId);

            LemmaFinder lemmaFinder = new LemmaFinder(new RussianLuceneMorphology());
            HashMap<String, Integer> lemmas = lemmaFinder.getLemmas(document);

            LemmaEntity lemma;
            int countWordOnPage = IndexEntity.getCountWordsOnPage(lemmas);
            float rank;

            for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                lemma = lemmaCRUDService.findByLemmaAndSiteId(entry.getKey(), siteId);
                if (lemma != null) {
                    lemma.setFrequency(lemma.getFrequency() + 1);
                    FindAndSaveLemmaAndIndex.getLemmasExistingInDB().add(lemma);
                } else {
                    lemma = new LemmaEntity();
                    lemma.setLemma(entry.getKey());
                    lemma.setSiteId(siteId);
                    lemma.setFrequency(1);
                    FindAndSaveLemmaAndIndex.getLemmasNotExistingInDB().add(lemma);
                }

                rank = IndexEntity.calculateRankValue(entry.getValue(), countWordOnPage);
                IndexEntity index = new IndexEntity();
                index.setPage(pageEntity);
                index.setLemma(lemma);
                index.setRank(rank);
                IndexEntity.getIndexes().add(index);
            }
            FindAndSaveLemmaAndIndex findAndSaveLemmaAndIndex = new FindAndSaveLemmaAndIndex(pageCRUDService,
                    lemmaCRUDService,
                    indexCRUDService);
            findAndSaveLemmaAndIndex.saveLemmaAndIndex();
            if (siteEntity.getStatusIndexing().equals(StatusIndexing.INDEXING)) {
                siteEntity.setStatusIndexing(StatusIndexing.INDEXED);
                siteCRUDService.save(siteEntity);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return new IndexingResponseTrue();

    }
}

