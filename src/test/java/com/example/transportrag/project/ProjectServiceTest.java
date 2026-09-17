package com.example.transportrag.project;

import com.example.transportrag.auth.AuthenticatedUserProvider;
import com.example.transportrag.audit.ResourceAccessAuditService;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectServiceTest {
    @Test
    void listsOnlyProjectsForAuthenticatedSubject() {
        ProjectMapper mapper = mock(ProjectMapper.class);
        AuthenticatedUserProvider userProvider = mock(AuthenticatedUserProvider.class);
        ResourceAccessAuditService auditService = mock(ResourceAccessAuditService.class);
        ProjectInfo project = new ProjectInfo("PRJ-COLD-HOKKAIDO", "北海道コールドチェーン改善",
                "冷蔵配送の改善", "MEMBER");
        when(userProvider.subject()).thenReturn("demo-cold");
        when(mapper.findBySubject("demo-cold")).thenReturn(List.of(project));

        assertThat(new ProjectService(mapper, userProvider, auditService).listMine()).containsExactly(project);
        verify(mapper).findBySubject("demo-cold");
        verify(auditService).recordProjects("demo-cold", List.of(project));
    }
}
