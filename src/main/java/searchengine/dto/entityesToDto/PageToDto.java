package searchengine.dto.entityesToDto;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

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
    private static HashMap<PageToDto, Integer> pageToDtoHashMap = new HashMap<>();

    public static PageToDto makePageToDtoForSave(Integer siteId, String url, String document, Integer code) {
        PageToDto pageToDto = new PageToDto();
        pageToDto.setSiteId(siteId);
        pageToDto.setPath(url);
        pageToDto.setContent(String.valueOf(document));
        pageToDto.setCode(code);
        return pageToDto;
    }

    public static void removePagesToDtoFromHashMap(ArrayList<PageToDto> pages) {
        for (PageToDto page : pages) {
            pageToDtoHashMap.remove(page);
        }
    }
    public synchronized static ArrayList<PageToDto> getPageToDtoArrayList() {
        ArrayList<PageToDto> list = new ArrayList<>(pageToDtoHashMap.keySet());
        return list;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        PageToDto page = (PageToDto) o;
        return this.siteId == page.getSiteId() && Objects.equals(this.path, page.getPath());
    }

    @Override
    public int hashCode() {
        int result = path == null ? 0 : path.hashCode();
        result = 31 * result + siteId;
        return result;
    }
}

