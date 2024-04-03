package searchengine.dto.entityesToDto;

import lombok.Getter;
import lombok.Setter;
import searchengine.model.entityes.SiteEntity;

@Getter
@Setter
public class PageToDto {

    private int id;
    private int siteId;
    private String path;
    private int code;
    private String content;
}

