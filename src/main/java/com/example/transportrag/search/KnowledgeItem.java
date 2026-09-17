package com.example.transportrag.search;

import java.time.LocalDate;

public record KnowledgeItem(
        String code,
        String kind,
        String title,
        String content,
        String ownerCode,
        String region,
        String aliases,
        String status,
        LocalDate validFrom,
        LocalDate validTo,
        String sourceUri) {
}
