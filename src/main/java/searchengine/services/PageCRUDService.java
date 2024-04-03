package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.catalina.util.Introspection;
import org.aspectj.apache.bcel.classfile.Module;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import searchengine.dto.entityesToDto.PageToDto;
import searchengine.model.entityes.PageEntity;
import searchengine.model.entityes.SiteEntity;
import searchengine.repository.PageRepository;

import java.util.Optional;

@Service
public class PageCRUDService {
    @Autowired
    private PageRepository pageRepository;

    public PageEntity getById(Integer id) {
        Optional<PageEntity> pageOptional = pageRepository.findById(id);
        if (pageOptional.isPresent()) {
            return pageOptional.get();
        }
        return null;
    }

    public void savePageStatusOk(SiteEntity site, Document doc, int code) {
        PageEntity page = new PageEntity();
        page.setSite(site);
        page.setPath(site.getUrl());
        page.setCode(code);
        page.setContent(doc.html());
        pageRepository.save(page);
    }
    public void savePageStatusError(SiteEntity site, int code) {
        PageEntity page = new PageEntity();
        page.setSite(site);
        page.setPath(site.getUrl());
        page.setCode(code);
        pageRepository.save(page);
    }
}
