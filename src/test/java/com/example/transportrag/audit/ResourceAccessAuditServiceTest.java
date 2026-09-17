package com.example.transportrag.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.transportrag.search.KnowledgeItem;
import com.example.transportrag.search.KnowledgeRelation;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ResourceAccessAuditServiceTest {
    private final ResourceAccessAuditMapper mapper = mock(ResourceAccessAuditMapper.class);
    private final ResourceAccessAuditService service = new ResourceAccessAuditService(mapper);

    @Test
    void recordsReturnedKnowledgeItemsUnderOneOperation() {
        KnowledgeItem first = item("RULE-001");
        KnowledgeItem second = item("RULE-002");

        service.recordKnowledgeItems("demo-cold", "SEARCH", "PRJ-COLD-HOKKAIDO", List.of(first, second));

        ArgumentCaptor<List<ResourceAccessAuditEntry>> captor = entriesCaptor();
        verify(mapper).insertAll(captor.capture());
        List<ResourceAccessAuditEntry> entries = captor.getValue();
        assertThat(entries).extracting(ResourceAccessAuditEntry::getResourceCode)
                .containsExactly("RULE-001", "RULE-002");
        assertThat(entries).allSatisfy(entry -> {
            assertThat(entry.getSubject()).isEqualTo("demo-cold");
            assertThat(entry.getAction()).isEqualTo("SEARCH");
            assertThat(entry.getRequestedProjectCode()).isEqualTo("PRJ-COLD-HOKKAIDO");
            assertThat(entry.getResourceType()).isEqualTo("KNOWLEDGE_ITEM");
            assertThat(entry.getOperationId()).isEqualTo(entries.getFirst().getOperationId());
        });
    }

    @Test
    void recordsRelationshipContext() {
        KnowledgeRelation relation = new KnowledgeRelation(1, "RULE-002", "SUPPORTS", "SVC-COLD",
                "温度管理を支える", "冷蔵輸送", "SERVICE");

        service.recordRelations("demo-cold", null, List.of(relation));

        ArgumentCaptor<List<ResourceAccessAuditEntry>> captor = entriesCaptor();
        verify(mapper).insertAll(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(entry -> {
            assertThat(entry.getAction()).isEqualTo("EXPLORE");
            assertThat(entry.getResourceType()).isEqualTo("KNOWLEDGE_RELATION");
            assertThat(entry.getResourceCode()).isEqualTo("SVC-COLD");
            assertThat(entry.getRelationFromCode()).isEqualTo("RULE-002");
            assertThat(entry.getRelationName()).isEqualTo("SUPPORTS");
        });
    }

    @Test
    void doesNotWriteRowsWhenNoResourceWasReturned() {
        service.recordKnowledgeItems("demo-cold", "GET", null, List.of());
        verifyNoInteractions(mapper);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static ArgumentCaptor<List<ResourceAccessAuditEntry>> entriesCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
    }

    private static KnowledgeItem item(String code) {
        return new KnowledgeItem(code, "RULE", code, "content", "ORG-QUALITY", "全国", "", "APPROVED",
                LocalDate.of(2026, 1, 1), null, "demo://audit/" + code);
    }
}
