package com.example.transportrag.audit;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ResourceAccessAuditMapper {
    int insertAll(@Param("entries") List<ResourceAccessAuditEntry> entries);
}
