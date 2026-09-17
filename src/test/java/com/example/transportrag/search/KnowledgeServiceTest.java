package com.example.transportrag.search;

import com.example.transportrag.auth.AuthenticatedUserProvider;
import com.example.transportrag.audit.ResourceAccessAuditService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class KnowledgeServiceTest {
    private final KnowledgeMapper mapper = mock(KnowledgeMapper.class);
    private final AuthenticatedUserProvider userProvider = mock(AuthenticatedUserProvider.class);
    private final ResourceAccessAuditService auditService = mock(ResourceAccessAuditService.class);
    private final KnowledgeService service = new KnowledgeService(mapper, userProvider, auditService);

    KnowledgeServiceTest() {
        when(userProvider.subject()).thenReturn("demo-cold");
    }

    @Test
    void normalizesFiltersAndBoundsResultLimit() {
        LocalDate date = LocalDate.of(2026, 9, 17);
        service.search("prj-cold-hokkaido", " 冷蔵 ", "rule", "北海道", "approved", date, 999);

        verify(mapper).search("demo-cold", "PRJ-COLD-HOKKAIDO", "冷蔵", "RULE", "北海道",
                "APPROVED", date, 50);
    }

    @Test
    void normalizesCodeAndBoundsGraphDepth() {
        LocalDate date = LocalDate.of(2026, 9, 17);
        service.related(null, "rule-002", 99, date, 0);

        verify(mapper).findRelated("demo-cold", null, "RULE-002", 3, date, 1);
    }

    @Test
    void rejectsCodeThatCouldChangeSqlOrGraphQueries() {
        assertThatThrownBy(() -> service.get(null, "RULE-002' OR true", LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(mapper);
    }

    @Test
    void returnsMapperResultsWithoutRewritingEvidence() {
        KnowledgeItem item = new KnowledgeItem("RULE-002", "RULE", "冷蔵品の温度管理",
                "2〜8度", "ORG-QUALITY", "全国", "", "APPROVED",
                LocalDate.of(2026, 1, 1), null, "demo://aozora/2026/RULE-002");
        when(mapper.search(any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(item));

        assertThat(service.search(null, "冷蔵", null, null, null, LocalDate.of(2026, 9, 17), 10))
                .containsExactly(item);
        verify(auditService).recordKnowledgeItems("demo-cold", "SEARCH", null, List.of(item));
    }

    @Test
    void rejectsMalformedProjectCode() {
        assertThatThrownBy(() -> service.search("OTHER-PROJECT", null, null, null, null,
                LocalDate.now(), 10)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(mapper);
    }
}
