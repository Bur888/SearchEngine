package searchengine.dto.searchResponse;

@lombok.Data
public class SearchResponse {
    private boolean result;
    private int count;
    private  Data data;
}
