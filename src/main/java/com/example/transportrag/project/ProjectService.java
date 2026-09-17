package com.example.transportrag.project;

import com.example.transportrag.auth.AuthenticatedUserProvider;
import com.example.transportrag.audit.ResourceAccessAuditService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {
    private final ProjectMapper mapper;
    private final AuthenticatedUserProvider userProvider;
    private final ResourceAccessAuditService auditService;

    public ProjectService(ProjectMapper mapper, AuthenticatedUserProvider userProvider,
            ResourceAccessAuditService auditService) {
        this.mapper = mapper;
        this.userProvider = userProvider;
        this.auditService = auditService;
    }

    @Transactional
    public List<ProjectInfo> listMine() {
        String subject = userProvider.subject();
        List<ProjectInfo> projects = mapper.findBySubject(subject);
        auditService.recordProjects(subject, projects);
        return projects;
    }
}
