package searchengine.dto.indexing;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IndexingResponseTrue implements IndexingResponse{
    private boolean result = true;
}
