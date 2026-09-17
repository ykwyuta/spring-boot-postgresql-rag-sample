CREATE EXTENSION IF NOT EXISTS pgroonga;
CREATE EXTENSION IF NOT EXISTS age;
LOAD 'age';
SET search_path = ag_catalog, public;

CREATE TABLE public.personal_access_tokens (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subject text NOT NULL,
    token_hash varchar(64) NOT NULL UNIQUE CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    description text NOT NULL DEFAULT '',
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    CHECK (expires_at > created_at)
);

CREATE TABLE public.business_rules (
    id varchar(32) PRIMARY KEY,
    title text NOT NULL,
    category text NOT NULL,
    region text NOT NULL,
    content text NOT NULL,
    effective_from date NOT NULL,
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX business_rules_attributes_idx ON public.business_rules (category, region);
CREATE INDEX business_rules_fulltext_idx ON public.business_rules USING pgroonga (title, content);
SELECT create_graph('transport_rules');
