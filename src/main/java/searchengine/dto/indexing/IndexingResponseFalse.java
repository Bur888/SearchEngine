package searchengine.dto.indexing;

import lombok.Getter;
import lombok.Setter;

@Getter
public class IndexingResponseFalse implements IndexingResponse {
    private static final boolean result = false;
    private static final String error = "Индексация уже запущена";
}
