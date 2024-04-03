package searchengine.model.searchLinks;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.model.entityes.SiteEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;


public class ParseWebRecursive extends RecursiveTask<Link> {

    @Getter
    @Setter
    private Link link;

    @Getter
    @Setter
    private SiteEntity siteEntity;

/*
    public ParseWebRecursive(Link link, SiteEntity siteEntity) {
        this.link = link;
        this.siteEntity = siteEntity;
    }
*/

    @Override
    protected Link compute() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        ParseWeb parseWed = new ParseWeb();
        parseWed.setUrl(link.getLink());
        parseWed.setSiteEntity(siteEntity);
        ArrayList<Link> links = parseWed.getLinksOnUrl();
        link.setLinks(links);
        List<ParseWebRecursive> taskList = new ArrayList<>();
        for (Link link : links) {
            ParseWebRecursive task = new ParseWebRecursive();
            task.setLink(link);
            task.setSiteEntity(siteEntity);
            task.fork();
            taskList.add(task);
        }
        for (RecursiveTask task : taskList) {
            task.join();
        }
        return link;
    }
/*
        for (Link link : links) {
            ParseWebRecursive task = new ParseWebRecursive(link, siteEntity);
            task.fork();
            taskList.add(task);
        }
        for (RecursiveTask task : taskList) {
            task.join();
        }
        return link;
    }
*/
}
