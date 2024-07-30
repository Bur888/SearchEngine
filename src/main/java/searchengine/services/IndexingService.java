package searchengine.services;

import lombok.Getter;
import lombok.Setter;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
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
import searchengine.model.IndexPage;
import searchengine.model.StartIndexingSites;
import searchengine.model.entityes.*;
import searchengine.model.findAndSaveLemmaAndIndex.FindAndSaveLemmaAndIndex;
import searchengine.model.findAndSaveLemmaAndIndex.LemmaFinder;
import searchengine.model.findAndSaveLemmaAndIndex.MultiLuceneMorphology;
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
    private Thread startIndexing;

    public IndexingResponse startIndexing() {

        if (startIndexingFlag) {
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
        if (!startIndexingFlag) {
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
        IndexPage indexPage = new IndexPage(siteCRUDService,
                                            pageCRUDService,
                                            lemmaCRUDService,
                                            indexCRUDService,
                                            sitesList);
        return indexPage.getResponse(url);
    }
}

