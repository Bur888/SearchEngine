package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.searchResponse.SearchResponse;
import searchengine.model.SearchWords;


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





