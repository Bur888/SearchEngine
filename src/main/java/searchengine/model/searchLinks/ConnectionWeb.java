package searchengine.model.searchLinks;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class ConnectionWeb {

    public Document getDocument(String url) throws IOException {
            return Jsoup
                    .connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (HTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11")
                    .referrer("http://www.google.com")
                    .timeout(10 * 1000)
                    .get();
    }
}
