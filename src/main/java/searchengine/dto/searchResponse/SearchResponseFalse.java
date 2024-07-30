package searchengine.dto.searchResponse;

import lombok.Data;

@Data
public class SearchResponseError implements SearchResponse {
    private String error;

    public SearchResponseError(String error) {
        this.error = error;
    }
}
