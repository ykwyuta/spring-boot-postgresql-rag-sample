package com.example.transportrag.audit;

/**
 * MyBatis parameter object for one resource access audit row.
 */
public final class ResourceAccessAuditEntry {
    private final String operationId;
    private final String subject;
    private final String action;
    private final String requestedProjectCode;
    private final String resourceType;
    private final String resourceCode;
    private final String relationFromCode;
    private final String relationName;

    public ResourceAccessAuditEntry(String operationId, String subject, String action, String requestedProjectCode,
            String resourceType, String resourceCode, String relationFromCode, String relationName) {
        this.operationId = operationId;
        this.subject = subject;
        this.action = action;
        this.requestedProjectCode = requestedProjectCode;
        this.resourceType = resourceType;
        this.resourceCode = resourceCode;
        this.relationFromCode = relationFromCode;
        this.relationName = relationName;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getSubject() {
        return subject;
    }

    public String getAction() {
        return action;
    }

    public String getRequestedProjectCode() {
        return requestedProjectCode;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceCode() {
        return resourceCode;
    }

    public String getRelationFromCode() {
        return relationFromCode;
    }

    public String getRelationName() {
        return relationName;
    }
}
