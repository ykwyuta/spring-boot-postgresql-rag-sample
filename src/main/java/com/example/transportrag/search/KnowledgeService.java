package com.example.transportrag.search;

import com.example.transportrag.auth.AuthenticatedUserProvider;
import com.example.transportrag.audit.ResourceAccessAuditService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeService {
    private static final int MAX_RESULTS = 50;
    private static final int MAX_DEPTH = 3;
    private final KnowledgeMapper mapper;
    private final AuthenticatedUserProvider userProvider;
    private final ResourceAccessAuditService auditService;

    public KnowledgeService(KnowledgeMapper mapper, AuthenticatedUserProvider userProvider,
            ResourceAccessAuditService auditService) {
        this.mapper = mapper;
        this.userProvider = userProvider;
        this.auditService = auditService;
    }

    @Transactional
    public KnowledgeItem get(String projectCode, String code, LocalDate asOf) {
        String subject = userProvider.subject();
        String normalizedProjectCode = projectCodeOrNull(projectCode);
        KnowledgeItem item = mapper.findByCode(subject, normalizedProjectCode, normalizeCode(code), dateOrToday(asOf));
        auditService.recordKnowledgeItems(subject, "GET", normalizedProjectCode,
                item == null ? List.of() : List.of(item));
        return item;
    }

    @Transactional
    public List<KnowledgeItem> search(String projectCode, String query, String kind, String region, String status,
            LocalDate asOf, Integer limit) {
        String subject = userProvider.subject();
        String normalizedProjectCode = projectCodeOrNull(projectCode);
        List<KnowledgeItem> items = mapper.search(subject, normalizedProjectCode, trimToNull(query),
                upperToNull(kind), trimToNull(region),
                upperToNull(status), dateOrToday(asOf), bounded(limit, 20, MAX_RESULTS));
        auditService.recordKnowledgeItems(subject, "SEARCH", normalizedProjectCode, items);
        return items;
    }

    @Transactional
    public List<KnowledgeRelation> related(String projectCode, String code, Integer depth,
            LocalDate asOf, Integer limit) {
        String subject = userProvider.subject();
        String normalizedProjectCode = projectCodeOrNull(projectCode);
        List<KnowledgeRelation> relations = mapper.findRelated(subject, normalizedProjectCode, normalizeCode(code),
                bounded(depth, 1, MAX_DEPTH), dateOrToday(asOf), bounded(limit, 30, MAX_RESULTS));
        auditService.recordRelations(subject, normalizedProjectCode, relations);
        return relations;
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
