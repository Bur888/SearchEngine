package searchengine.model.searchLinks;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.services.PageCRUDService;
import searchengine.services.SiteCRUDService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;
import java.util.logging.Logger;


public class ParseWebRecursive extends RecursiveTask<Link> {

    @Autowired
    private SiteCRUDService siteCRUDService;
    private PageCRUDService pageCRUDService;

    @Getter
    @Setter
    private Link link;

    @Autowired
    public ParseWebRecursive(SiteCRUDService siteCRUDService, PageCRUDService pageCRUDService) {
        this.siteCRUDService = siteCRUDService;
        this.pageCRUDService = pageCRUDService;
    }

    @Override
    protected Link compute() {
        Logger.getLogger("123").info("Организован поиск по ссылке " + link.getUrl());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        ParseWeb parseWed = new ParseWeb(siteCRUDService, pageCRUDService);
        parseWed.setLink(link);
        ArrayList<Link> links = parseWed.getLinksOnUrl();
        link.setLinks(links);
        List<ParseWebRecursive> taskList = new ArrayList<>();
        for (Link link : links) {
            ParseWebRecursive task = new ParseWebRecursive(siteCRUDService, pageCRUDService);
            task.setLink(link);
            task.fork();
            taskList.add(task);
        }
        for (RecursiveTask task : taskList) {
            task.join();
        }
        return link;
    }
}
