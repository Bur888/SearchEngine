package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private PageCRUDService pageCRUDService;
    private SiteCRUDService siteCRUDService;
    private LemmaCRUDService lemmaCRUDService;

    @Autowired
    public StatisticsServiceImpl(SitesList sites, PageCRUDService pageCRUDService, SiteCRUDService siteCRUDService, LemmaCRUDService lemmaCRUDService) {
        this.sites = sites;
        this.pageCRUDService = pageCRUDService;
        this.siteCRUDService = siteCRUDService;
        this.lemmaCRUDService = lemmaCRUDService;
    }

    @Override
    public StatisticsResponse getStatistics() {
        String[] statuses = { "INDEXED", "FAILED", "INDEXING" };
        String[] errors = {
                "Ошибка индексации: главная страница сайта не доступна",
                "Ошибка индексации: сайт не доступен",
                ""
        };

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            int siteId = siteCRUDService.getIdByUrl(site.getUrl());
            int pages = pageCRUDService.getCountPagesOnSite(siteId);
            int lemmas = lemmaCRUDService.getCountLemmasOnSite(siteId);
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(siteCRUDService.getStatusIndexing(siteId));
            item.setError(siteCRUDService.getErrorIndexing(siteId));
            item.setStatusTime(siteCRUDService.getStatusTimeIndexing(siteId)
                    .atZone(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli());
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
