package searchengine.model.searchLinks;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.services.LemmaCRUDService;
import searchengine.services.PageCRUDService;
import searchengine.services.SiteCRUDService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;
import java.util.logging.Logger;

public class ParseWebRecursive extends RecursiveTask<Link> {

    @Getter
    @Setter
    private static boolean stopNow;
    private final SiteCRUDService siteCRUDService;
    private final PageCRUDService pageCRUDService;
    private final LemmaCRUDService lemmaCRUDService;

    @Getter
    @Setter
    private Link link;

    @Autowired
    public ParseWebRecursive(SiteCRUDService siteCRUDService,
                             PageCRUDService pageCRUDService,
                             LemmaCRUDService lemmaCRUDService) {
        this.siteCRUDService = siteCRUDService;
        this.pageCRUDService = pageCRUDService;
        this.lemmaCRUDService = lemmaCRUDService;
    }

    @Override
    protected Link compute() {
        if (stopNow) {
            return null;
        }
        Logger.getLogger("123").info("Организован поиск по ссылке " + link.getUrl());
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        ParseWeb parseWed = new ParseWeb(siteCRUDService, pageCRUDService, lemmaCRUDService);
        parseWed.setLink(link);
        ArrayList<Link> links = parseWed.getLinksOnUrl();
        link.setLinks(links);
        List<ParseWebRecursive> taskList = new ArrayList<>();
        for (Link link : links) {
            ParseWebRecursive task = new ParseWebRecursive(siteCRUDService, pageCRUDService, lemmaCRUDService);
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
