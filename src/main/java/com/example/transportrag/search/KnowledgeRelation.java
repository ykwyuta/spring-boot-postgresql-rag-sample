package com.example.transportrag.search;

public record KnowledgeRelation(
        int depth,
        String fromCode,
        String relation,
        String toCode,
        String rationale,
        String title,
        String kind) {
}
