package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.dto.statistics.StatisticsResponse;


public interface StatisticsService {
    StatisticsResponse getStatistics();
}
