package searchengine.dto.entityesToDto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import searchengine.model.entityes.SiteEntity;
import searchengine.model.searchLinks.Link;

import java.net.MalformedURLException;
import java.net.URL;
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

    public PageToDto makePageToDtoForSave(Integer siteId, String url, String document, Integer code) {
        PageToDto pageToDto = new PageToDto();
        pageToDto.setSiteId(siteId);
        pageToDto.setPath(url);
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

