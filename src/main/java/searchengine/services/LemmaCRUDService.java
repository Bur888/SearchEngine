package searchengine.services;

import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import searchengine.dto.searchResponse.SearchResponse;
import searchengine.model.findAndSaveLemmaAndIndex.LemmaFinder;
import searchengine.model.entityes.LemmaEntity;
import searchengine.model.findAndSaveLemmaAndIndex.MultiLuceneMorphology;
import searchengine.repository.LemmaRepository;

import org.jsoup.nodes.Document;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LemmaCRUDService {
    private LemmaRepository lemmaRepository;
    private SiteCRUDService siteCRUDService;
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public LemmaCRUDService(LemmaRepository lemmaRepository,SiteCRUDService siteCRUDService, JdbcTemplate jdbcTemplate) {
        this.lemmaRepository = lemmaRepository;
        this.siteCRUDService = siteCRUDService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<LemmaEntity> findAllByLemma(String lemma) {
        return lemmaRepository.findAllByLemma(lemma);
    }

    public LemmaEntity findByLemmaAndSiteId(String lemma, int siteId) {
        return lemmaRepository.findOneByLemmaAndSiteId(lemma, siteId);
    }

    public List<LemmaEntity> findAllByLemmas (List<String> lemmas) {
        List<LemmaEntity> lemmaEntityListResult = new ArrayList<>();
        List<LemmaEntity> lemmaEntityList;
        for (String lemma : lemmas) {
            lemmaEntityList = findAllByLemma(lemma);
            if (lemmaEntityList.isEmpty()) {
                lemmaEntityListResult.add(null);
            } else {
                lemmaEntityListResult.addAll(lemmaEntityList);
            }
        }
        return lemmaEntityListResult;
    }

    public LemmaEntity findByLemmaAndUrl(String lemma, String url) {
        int siteId = siteCRUDService.getIdByUrl(url);
        return lemmaRepository.findOneByLemmaAndSiteId(lemma, siteId);
    }

    public List<LemmaEntity> findLemmasByPageId(int pageId) {
        return lemmaRepository.findLemmasByPageId(pageId);
    }

    public HashSet<LemmaEntity> findByLemmaAndSiteId(HashSet<LemmaEntity> lemmas) {
        HashSet<LemmaEntity> lemmasFromDB = new HashSet<>();
        //Выбираем из списка lemmas уникальные site_id
        HashSet<Integer> distinctSiteId = (HashSet<Integer>) lemmas.stream()
                .map(LemmaEntity::getSiteId)
                .collect(Collectors.toSet());

        //Для каждого site_id выполняем запрос к базе данных
        for (Integer siteId : distinctSiteId) {
        String sql = "SELECT * FROM search_engine.lemma WHERE site_id = ? AND lemma IN (" +
                lemmas.stream()
                        .filter(lemma -> lemma.getSiteId() == siteId)
                        .map(lemma -> "'" + lemma.getLemma() + "'")
                        .collect(Collectors.joining(",")) +
                ")";
        List<LemmaEntity> lemmasList = jdbcTemplate.query(sql, (rs, rowNum) -> {
            LemmaEntity lemma = new LemmaEntity();
            lemma.setId(rs.getInt("id"));
            lemma.setFrequency(rs.getInt("frequency"));
            lemma.setLemma(rs.getString("lemma"));
            lemma.setSiteId(rs.getInt("site_id"));
            return lemma;
        }, siteId.toString());
        //добавляем леммы из БД в HashSet
        lemmasFromDB.addAll(lemmasList);
    }
        return lemmasFromDB;
    }

    public void save(LemmaEntity lemma) {
        lemmaRepository.save(lemma);
    }

    public HashSet<LemmaEntity> saveAll(HashSet<LemmaEntity> lemmas) {
        jdbcTemplate.batchUpdate("INSERT INTO search_engine.lemma (site_id, lemma, frequency) " +
                        "VALUES (?, ?, ?)",
                lemmas,
                lemmas.size(),
                (PreparedStatement ps, LemmaEntity lemmaEntity) -> {
                    ps.setInt(1, lemmaEntity.getSiteId());
                    ps.setString(2, lemmaEntity.getLemma());
                    ps.setInt(3, lemmaEntity.getFrequency());
                });

        return findByLemmaAndSiteId(lemmas);
    }

    public HashSet<LemmaEntity> updateAll(HashSet<LemmaEntity> lemmas) {
        jdbcTemplate.batchUpdate("UPDATE lemma SET frequency = ? WHERE id = ?",
                lemmas,
                lemmas.size(),
                (PreparedStatement ps, LemmaEntity lemmaEntity) -> {
                    ps.setInt(1, lemmaEntity.getFrequency());
                    ps.setInt(2, lemmaEntity.getId());
                });

        return findByLemmaAndSiteId(lemmas);
    }

    public LemmaEntity findById(int lemmaId) {
        Optional<LemmaEntity> lemma = lemmaRepository.findById(lemmaId);
        return lemma.orElse(null);
    }

    public void delete(LemmaEntity lemma) {
        lemmaRepository.delete(lemma);
    }

    public List<LemmaEntity> findAllBySiteId(int siteId) {
        return lemmaRepository.findAllBySiteId(siteId);
    }

    public Integer getCountLemmasOnSite(int siteId) {
        return lemmaRepository.getCountLemmasOnSite(siteId);
    }

    public Integer getCountPagesWithLemma(String lemma){
        return lemmaRepository.getCountPagesWithLemma(lemma);
    }

    public void findAndSaveLemma(Document document, int siteId) {
        try {
            MultiLuceneMorphology multiLuceneMorphology
                    = new MultiLuceneMorphology(new RussianLuceneMorphology(), new EnglishLuceneMorphology());
            LemmaFinder lemmaFinder = new LemmaFinder(multiLuceneMorphology);
            HashMap<String, Integer> lemmas = lemmaFinder.getLemmas(document);
            LemmaEntity lemma;

            for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                lemma = findByLemmaAndSiteId(entry.getKey(), siteId);
                if (lemma != null) {
                    int newFrequency = lemma.getFrequency() + 1;
                    lemma.setFrequency(newFrequency);
                } else {
                    lemma = new LemmaEntity();
                    lemma.setLemma(entry.getKey());
                    lemma.setSiteId(siteId);
                    lemma.setFrequency(1);
                }
                save(lemma);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
