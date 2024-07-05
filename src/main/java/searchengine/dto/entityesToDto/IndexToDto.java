package searchengine.dto.entityesToDto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IndexToDto {

    private int id;

    private int pageID;

    private int lemmaId;

    private float rank;
}
