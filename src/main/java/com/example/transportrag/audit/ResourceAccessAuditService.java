package com.example.transportrag.audit;

import com.example.transportrag.project.ProjectInfo;
import com.example.transportrag.search.KnowledgeItem;
import com.example.transportrag.search.KnowledgeRelation;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ResourceAccessAuditService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceAccessAuditService.class);
    private static final String PROJECT = "PROJECT";
    private static final String KNOWLEDGE_ITEM = "KNOWLEDGE_ITEM";
    private static final String KNOWLEDGE_RELATION = "KNOWLEDGE_RELATION";

    private final ResourceAccessAuditMapper mapper;

    public ResourceAccessAuditService(ResourceAccessAuditMapper mapper) {
        this.mapper = mapper;
    }

    public void recordProjects(String subject, List<ProjectInfo> projects) {
        String operationId = UUID.randomUUID().toString();
        insert(projects.stream()
                .map(project -> new ResourceAccessAuditEntry(operationId, subject, "LIST_PROJECTS", null,
                        PROJECT, project.code(), null, null))
                .toList());
    }

    public void recordKnowledgeItems(String subject, String action, String requestedProjectCode,
            List<KnowledgeItem> items) {
        String operationId = UUID.randomUUID().toString();
        insert(items.stream()
                .map(item -> new ResourceAccessAuditEntry(operationId, subject, action, requestedProjectCode,
                        KNOWLEDGE_ITEM, item.code(), null, null))
                .toList());
    }

    public void recordRelations(String subject, String requestedProjectCode, List<KnowledgeRelation> relations) {
        String operationId = UUID.randomUUID().toString();
        insert(relations.stream()
                .map(relation -> new ResourceAccessAuditEntry(operationId, subject, "EXPLORE", requestedProjectCode,
                        KNOWLEDGE_RELATION, relation.toCode(), relation.fromCode(), relation.relation()))
                .toList());
    }

    private void insert(List<ResourceAccessAuditEntry> entries) {
        if (!entries.isEmpty()) {
            try {
                mapper.insertAll(entries);
            } catch (RuntimeException exception) {
                LOGGER.error("Failed to persist resource access audit rows", exception);
                throw exception;
            }
        }
    }
}
