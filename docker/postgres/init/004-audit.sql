CREATE TABLE public.resource_access_audit_log (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    operation_id uuid NOT NULL,
    accessed_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subject varchar(128) NOT NULL,
    action varchar(32) NOT NULL CHECK (action IN ('LIST_PROJECTS', 'SEARCH', 'GET', 'EXPLORE')),
    requested_project_code varchar(64),
    resource_type varchar(32) NOT NULL
        CHECK (resource_type IN ('PROJECT', 'KNOWLEDGE_ITEM', 'KNOWLEDGE_RELATION')),
    resource_code varchar(64) NOT NULL,
    relation_from_code varchar(64),
    relation_name varchar(32),
    CHECK (
        (resource_type = 'KNOWLEDGE_RELATION' AND relation_from_code IS NOT NULL AND relation_name IS NOT NULL)
        OR
        (resource_type <> 'KNOWLEDGE_RELATION' AND relation_from_code IS NULL AND relation_name IS NULL)
    )
);

CREATE INDEX resource_access_audit_subject_time_idx
    ON public.resource_access_audit_log (subject, accessed_at DESC);
CREATE INDEX resource_access_audit_resource_time_idx
    ON public.resource_access_audit_log (resource_type, resource_code, accessed_at DESC);
CREATE INDEX resource_access_audit_operation_idx
    ON public.resource_access_audit_log (operation_id);

COMMENT ON TABLE public.resource_access_audit_log IS
    'Append-only audit records for resources returned by authenticated MCP tools.';
COMMENT ON COLUMN public.resource_access_audit_log.subject IS
    'Authenticated subject snapshot; intentionally not a foreign key so account removal does not erase audit identity.';
