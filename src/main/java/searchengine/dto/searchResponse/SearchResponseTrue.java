package searchengine.dto.searchResponse;

import java.util.List;

@lombok.Data
public class SearchResponseTrue implements SearchResponse {
    private boolean result = true;
    private int count;
    private List<Data> data;
}
