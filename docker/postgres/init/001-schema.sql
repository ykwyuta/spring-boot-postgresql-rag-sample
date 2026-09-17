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

CREATE TABLE public.knowledge_items (
    code varchar(64) PRIMARY KEY,
    kind varchar(32) NOT NULL,
    title text NOT NULL,
    content text NOT NULL,
    owner_code varchar(64),
    region text NOT NULL,
    aliases text NOT NULL DEFAULT '',
    status varchar(16) NOT NULL,
    valid_from date NOT NULL,
    valid_to date,
    source_uri text NOT NULL,
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    CHECK (valid_to IS NULL OR valid_to > valid_from)
);
CREATE INDEX knowledge_items_attributes_idx
    ON public.knowledge_items (kind, region, status, valid_from, valid_to);
CREATE INDEX knowledge_items_fulltext_idx
    ON public.knowledge_items USING pgroonga (title, content, aliases);

CREATE TABLE public.knowledge_relations (
    source_code varchar(64) NOT NULL REFERENCES public.knowledge_items(code),
    target_code varchar(64) NOT NULL REFERENCES public.knowledge_items(code),
    relation varchar(32) NOT NULL,
    rationale text NOT NULL,
    PRIMARY KEY (source_code, target_code, relation)
);
CREATE INDEX knowledge_relations_target_idx
    ON public.knowledge_relations (target_code, relation);

SELECT create_graph('business_knowledge');
