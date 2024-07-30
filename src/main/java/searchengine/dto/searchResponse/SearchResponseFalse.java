package searchengine.dto.searchResponse;

import lombok.Data;

@Data
public class SearchResponseFalse implements SearchResponse {
    private String error;

    public SearchResponseFalse(String error) {
        this.error = error;
    }
}
