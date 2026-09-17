package com.example.transportrag.project;

import com.example.transportrag.auth.AuthenticatedUserProvider;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {
    private final ProjectMapper mapper;
    private final AuthenticatedUserProvider userProvider;

    public ProjectService(ProjectMapper mapper, AuthenticatedUserProvider userProvider) {
        this.mapper = mapper;
        this.userProvider = userProvider;
    }

    public List<ProjectInfo> listMine() {
        return mapper.findBySubject(userProvider.subject());
    }
}
