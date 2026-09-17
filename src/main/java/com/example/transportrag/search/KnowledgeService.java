package com.example.transportrag.search;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeService {
    private static final int MAX_RESULTS = 50;
    private static final int MAX_DEPTH = 3;
    private final KnowledgeMapper mapper;

    public KnowledgeService(KnowledgeMapper mapper) {
        this.mapper = mapper;
    }

    public KnowledgeItem get(String code, LocalDate asOf) {
        return mapper.findByCode(normalizeCode(code), dateOrToday(asOf));
    }

    public List<KnowledgeItem> search(String query, String kind, String region, String status,
            LocalDate asOf, Integer limit) {
        return mapper.search(trimToNull(query), upperToNull(kind), trimToNull(region),
                upperToNull(status), dateOrToday(asOf), bounded(limit, 20, MAX_RESULTS));
    }

    public List<KnowledgeRelation> related(String code, Integer depth, LocalDate asOf, Integer limit) {
        return mapper.findRelated(normalizeCode(code), bounded(depth, 1, MAX_DEPTH),
                dateOrToday(asOf), bounded(limit, 30, MAX_RESULTS));
    }

    private static String normalizeCode(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]{2,64}")) {
            throw new IllegalArgumentException("code must contain 2-64 letters, numbers, '_' or '-'");
        }
        return value.toUpperCase();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String upperToNull(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private static LocalDate dateOrToday(LocalDate value) {
        return value == null ? LocalDate.now() : value;
    }

    private static int bounded(Integer value, int defaultValue, int maximum) {
        return value == null ? defaultValue : Math.max(1, Math.min(value, maximum));
    }
}
