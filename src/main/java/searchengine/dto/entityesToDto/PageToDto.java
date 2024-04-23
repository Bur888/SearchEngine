package searchengine.dto.entityesToDto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import searchengine.model.searchLinks.Link;
import java.util.ArrayList;

@Getter
@Setter
public class PageToDto {

    private int id;
    private int siteId;
    private String path;
    private int code;
    private String content;
    @Getter
    @Setter
    private static ArrayList<PageToDto> pageToDtoList = new ArrayList<>();

/*
    public static ArrayList<PageToDto> getPageToDtoList() {
        return pageToDtoList;
    }

    public synchronized static void setPageToDtoList(ArrayList<PageToDto> pageToDtoList) {
        PageToDto.pageToDtoList = pageToDtoList;
    }
*/

    public PageToDto makePageToDtoForSave(Link link, String document, Integer code) {
        PageToDto pageToDto = new PageToDto();
        pageToDto.setSiteId(link.getSiteId());
        pageToDto.setPath(link.getUrl());
        pageToDto.setContent(String.valueOf(document));
        pageToDto.setCode(code);
        return pageToDto;
    }
    public static void removePagesToDtoFromList(int count) {
        for (int i = 0; i < count; i++) {
            pageToDtoList.remove(0);
        }
    }
}

