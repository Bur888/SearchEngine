package searchengine.dto.indexing;

import lombok.Getter;

@Getter
public class IndexingResponseFalse implements IndexingResponse {
    private static final boolean result = false;
    private String error;

    public IndexingResponseFalse(String error) {
        this.error = error;
    }
}
