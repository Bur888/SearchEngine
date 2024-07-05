package searchengine.model.searchLinks;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.HttpStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.config.Site;
import searchengine.dto.entityesToDto.PageToDto;
import searchengine.model.entityes.SiteEntity;
import searchengine.model.entityes.StatusIndexing;
import searchengine.model.findAndSaveLemmaAndIndex.FindAndSaveLemmaAndIndex;
import searchengine.services.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.logging.Logger;


public class ThreadForSearchLinks implements Runnable {
    private SiteCRUDService siteCRUDService;
    private PageCRUDService pageCRUDService;
    private LemmaCRUDService lemmaCRUDService;
    private IndexCRUDService indexCRUDService;
    @Getter
    @Setter
    private Site site;

    @Autowired
    public ThreadForSearchLinks(SiteCRUDService siteCRUDService,
                                PageCRUDService pageCRUDService,
                                LemmaCRUDService lemmaCRUDService,
                                IndexCRUDService indexCRUDService,
                                Site site) {
        this.siteCRUDService = siteCRUDService;
        this.pageCRUDService = pageCRUDService;
        this.lemmaCRUDService = lemmaCRUDService;
        this.indexCRUDService = indexCRUDService;
        this.site = site;
    }

    @Override
    public void run() {
        try {
            String urlAndRoot = site.getUrl();
            Integer siteId = siteCRUDService.getIdByUrl(site.getUrl());
            if (siteId != null) {
                lemmaCRUDService.findAllBySiteId(siteId).forEach(lemma -> lemmaCRUDService.delete(lemma));
                siteCRUDService.deleteById(siteId);
            }

            SiteEntity newSite = new SiteEntity();
            newSite.setUrl(site.getUrl());
            newSite.setName(site.getName());
            newSite.setStatusIndexing(StatusIndexing.INDEXING);
            siteCRUDService.save(newSite);
            Link.getRootUrls().add(site.getUrl());

            isUrlWorking(newSite.getUrl());
            Integer newSiteId = siteCRUDService.getIdByUrl(urlAndRoot);

            //Добавляю счетчик индексируемых сайтов. При завершении индексации колличество будет уменьшаться на один. Это необходимо для финишного сохранения индексов, т.к. у меня стоит условие на сохранение индексов в базу данных в классе FindAndSaveLemmaAndIndex не менее 500.
            synchronized (FindAndSaveLemmaAndIndex.class) {
                FindAndSaveLemmaAndIndex.setFinishSave(FindAndSaveLemmaAndIndex.getFinishSave() + 1);
            }

            Link newLink = new Link(urlAndRoot, newSiteId, urlAndRoot);
            ParseWebRecursive newTask = new ParseWebRecursive(siteCRUDService, pageCRUDService, lemmaCRUDService);
            newTask.setLink(newLink);
            newTask.fork();
            Logger.getLogger(String.valueOf(ThreadForSearchLinks.class))
                    .info("ThreadForSearchLinks  " + site.getUrl() + " начался обход сайта");

            newTask.join();
            Thread.sleep(3100);
            Logger.getLogger(String.valueOf(ThreadForSearchLinks.class))
                    .info("Поиск по ссылке " + urlAndRoot + " завершен");
            ArrayList<PageToDto> pagesListForSave = PageToDto.getPageToDtoArrayList();
            pageCRUDService.saveAndRemove(pagesListForSave);
            FindAndSaveLemmaAndIndex findAndSaveLemmaAndIndex
                    = new FindAndSaveLemmaAndIndex(pageCRUDService, lemmaCRUDService, indexCRUDService);

            synchronized (FindAndSaveLemmaAndIndex.class) {
                findAndSaveLemmaAndIndex.run();
                FindAndSaveLemmaAndIndex.setFinishSave(FindAndSaveLemmaAndIndex.getFinishSave() - 1);
                if (FindAndSaveLemmaAndIndex.getFinishSave() == 0) {
                    findAndSaveLemmaAndIndex.saveLemmaAndIndex();
                }
            }
            newSite.setStatusIndexing(StatusIndexing.INDEXED);
            newSite.setStatusTime(LocalDateTime.now());
            siteCRUDService.save(newSite);
            Logger.getLogger(String.valueOf(ThreadForSearchLinks.class))
                    .info("Статус индексации ссылки " + urlAndRoot + " изменен");
        } catch (HttpStatusException ex) {
            siteCRUDService.updateWithFailedStatus(site.getUrl(), getHttpStatusException(ex.getStatusCode()));
        } catch (IOException ex) {
            siteCRUDService.updateWithFailedStatus(site.getUrl(), "Отсутствует соединение");
        } catch (InterruptedException ex) {
            siteCRUDService.updateWithFailedStatus(site.getUrl(), "Индексация остановлена пользователем");
            Logger.getLogger(String.valueOf(IndexingService.class))
                    .info("Поиск по ссылке " + site.getUrl() + " прерван");
        }
    }

    public void isUrlWorking(String url) throws IOException {
        ConnectionWeb connection = new ConnectionWeb();
        connection.getDocument(url);
    }

    public String getHttpStatusException(Integer code) {
        String error;
        switch (code) {
            case (403) -> error = "Исключение HttpStatusException: ошибка HTTP при получении URL-адреса. " +
                    "Статус = 403. Доступ к сайту запрещен";
            case (404) -> error = "Исключение HttpStatusException: ошибка HTTP при получении URL-адреса. " +
                    "Статус = 404. Сервер не может найти нужную страницу";
            default -> error = "Исключение HttpStatusException. Статус = " + code;
        }
        return error;
    }
}
