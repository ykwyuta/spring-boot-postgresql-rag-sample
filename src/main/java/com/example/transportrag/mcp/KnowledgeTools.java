package com.example.transportrag.mcp;

import com.example.transportrag.search.KnowledgeItem;
import com.example.transportrag.search.KnowledgeRelation;
import com.example.transportrag.search.KnowledgeService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeTools {
    private final KnowledgeService service;

    public KnowledgeTools(KnowledgeService service) {
        this.service = service;
    }

    @McpTool(name = "search_business_knowledge", description = "青空運送の規程、用語、組織、業務フロー、サービス、ニーズ、KPI、業界・競合シナリオを日本語全文・属性で検索します")
    public List<KnowledgeItem> search(
            @McpToolParam(description = "検索語。省略時は属性だけで検索", required = false) String query,
            @McpToolParam(description = "種別。CODE, TERM, ORG, FLOW, SERVICE, PARTNER, RULE, MANAGEMENT_NEED, CUSTOMER_NEED, FIELD_NEED, KPI, INDUSTRY, COMPETITOR", required = false) String kind,
            @McpToolParam(description = "地域。北海道、関東、全国など", required = false) String region,
            @McpToolParam(description = "状態。APPROVED, PROPOSED, OBSERVATION, RETIRED", required = false) String status,
            @McpToolParam(description = "基準日（YYYY-MM-DD）。省略時は今日", required = false) String asOf,
            @McpToolParam(description = "最大件数（1〜50）", required = false) Integer limit) {
        return service.search(query, kind, region, status, parseDate(asOf), limit);
    }

    @McpTool(name = "get_business_knowledge", description = "コードで知識項目を取得し、出典・有効期間・主管組織を確認します")
    public KnowledgeItem get(
            @McpToolParam(description = "RULE-002やTERM-OTIFなどのコード", required = true) String code,
            @McpToolParam(description = "基準日（YYYY-MM-DD）。省略時は今日", required = false) String asOf) {
        return service.get(code, parseDate(asOf));
    }

    @McpTool(name = "explore_business_relationships", description = "コード体系、組織、フロー、規程、サービス、ニーズ、KPI、業界・競合の関係を探索します")
    public List<KnowledgeRelation> related(
            @McpToolParam(description = "探索開始コード", required = true) String code,
            @McpToolParam(description = "探索深度（1〜3）", required = false) Integer depth,
            @McpToolParam(description = "基準日（YYYY-MM-DD）。省略時は今日", required = false) String asOf,
            @McpToolParam(description = "最大件数（1〜50）", required = false) Integer limit) {
        return service.related(code, depth, parseDate(asOf), limit);
    }

    private static LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }
}
