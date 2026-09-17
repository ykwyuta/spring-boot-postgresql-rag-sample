package com.example.transportrag.project;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProjectMapper {
    List<ProjectInfo> findBySubject(@Param("subject") String subject);
}
