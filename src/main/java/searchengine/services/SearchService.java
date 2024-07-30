package searchengine.services;

import org.apache.catalina.util.Introspection;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.WrongCharaterException;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.hibernate.type.StringNVarcharType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.searchResponse.Data;
import searchengine.dto.searchResponse.SearchResponse;
import searchengine.dto.searchResponse.SearchResponseFalse;
import searchengine.dto.searchResponse.SearchResponseTrue;
import searchengine.model.SearchWords;
import searchengine.model.entityes.IndexEntity;
import searchengine.model.entityes.LemmaEntity;
import searchengine.model.entityes.PageEntity;
import searchengine.model.entityes.SiteEntity;
import searchengine.model.findAndSaveLemmaAndIndex.LemmaFinder;
import searchengine.model.findAndSaveLemmaAndIndex.MultiLuceneMorphology;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


@Service
public class SearchService {

    private LemmaCRUDService lemmaCRUDService;
    private PageCRUDService pageCRUDService;
    private IndexCRUDService indexCRUDService;
    private SiteCRUDService siteCRUDService;

    @Autowired
    public SearchService(LemmaCRUDService lemmaCRUDService,
                         PageCRUDService pageCRUDService,
                         IndexCRUDService indexCRUDService,
                         SiteCRUDService siteCRUDService) {
        this.lemmaCRUDService = lemmaCRUDService;
        this.pageCRUDService = pageCRUDService;
        this.indexCRUDService = indexCRUDService;
        this.siteCRUDService = siteCRUDService;
    }

    public SearchResponse search(String query, String site, Integer offset, Integer limit) {
        SearchWords searchWords = new SearchWords(lemmaCRUDService,
                                                  pageCRUDService,
                                                  indexCRUDService,
                                                  siteCRUDService);
        return searchWords.search(query, site, offset, limit);
    }
}





