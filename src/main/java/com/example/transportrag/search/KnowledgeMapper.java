package com.example.transportrag.search;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface KnowledgeMapper {
    KnowledgeItem findByCode(@Param("code") String code, @Param("asOf") LocalDate asOf);

    List<KnowledgeItem> search(
            @Param("query") String query,
            @Param("kind") String kind,
            @Param("region") String region,
            @Param("status") String status,
            @Param("asOf") LocalDate asOf,
            @Param("limit") int limit);

    List<KnowledgeRelation> findRelated(
            @Param("code") String code,
            @Param("depth") int depth,
            @Param("asOf") LocalDate asOf,
            @Param("limit") int limit);
}
