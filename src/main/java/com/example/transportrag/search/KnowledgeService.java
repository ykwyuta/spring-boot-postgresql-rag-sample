package com.example.transportrag.search;

import com.example.transportrag.auth.AuthenticatedUserProvider;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeService {
    private static final int MAX_RESULTS = 50;
    private static final int MAX_DEPTH = 3;
    private final KnowledgeMapper mapper;
    private final AuthenticatedUserProvider userProvider;

    public KnowledgeService(KnowledgeMapper mapper, AuthenticatedUserProvider userProvider) {
        this.mapper = mapper;
        this.userProvider = userProvider;
    }

    public KnowledgeItem get(String projectCode, String code, LocalDate asOf) {
        return mapper.findByCode(userProvider.subject(), projectCodeOrNull(projectCode),
                normalizeCode(code), dateOrToday(asOf));
    }

    public List<KnowledgeItem> search(String projectCode, String query, String kind, String region, String status,
            LocalDate asOf, Integer limit) {
        return mapper.search(userProvider.subject(), projectCodeOrNull(projectCode), trimToNull(query),
                upperToNull(kind), trimToNull(region),
                upperToNull(status), dateOrToday(asOf), bounded(limit, 20, MAX_RESULTS));
    }

    public List<KnowledgeRelation> related(String projectCode, String code, Integer depth,
            LocalDate asOf, Integer limit) {
        return mapper.findRelated(userProvider.subject(), projectCodeOrNull(projectCode), normalizeCode(code),
                bounded(depth, 1, MAX_DEPTH), dateOrToday(asOf), bounded(limit, 30, MAX_RESULTS));
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

    private static String projectCodeOrNull(String value) {
        String normalized = upperToNull(value);
        if (normalized != null && !normalized.matches("PRJ-[A-Z0-9-]{1,60}")) {
            throw new IllegalArgumentException("projectCode must use the PRJ- prefix");
        }
        return normalized;
    }

    private static LocalDate dateOrToday(LocalDate value) {
        return value == null ? LocalDate.now() : value;
    }

    private static int bounded(Integer value, int defaultValue, int maximum) {
        return value == null ? defaultValue : Math.max(1, Math.min(value, maximum));
    }
}
