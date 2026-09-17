CREATE EXTENSION IF NOT EXISTS pgroonga;
CREATE EXTENSION IF NOT EXISTS age;
LOAD 'age';
SET search_path = ag_catalog, public;

CREATE TABLE public.app_users (
    subject varchar(128) PRIMARY KEY,
    display_name text NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.projects (
    code varchar(64) PRIMARY KEY,
    name text NOT NULL,
    description text NOT NULL,
    active boolean NOT NULL DEFAULT true
);

CREATE TABLE public.user_project_memberships (
    subject varchar(128) NOT NULL REFERENCES public.app_users(subject),
    project_code varchar(64) NOT NULL REFERENCES public.projects(code),
    project_role varchar(32) NOT NULL DEFAULT 'MEMBER',
    PRIMARY KEY (subject, project_code)
);

CREATE TABLE public.personal_access_tokens (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subject varchar(128) NOT NULL REFERENCES public.app_users(subject),
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

CREATE TABLE public.knowledge_item_projects (
    item_code varchar(64) NOT NULL REFERENCES public.knowledge_items(code),
    project_code varchar(64) NOT NULL REFERENCES public.projects(code),
    PRIMARY KEY (item_code, project_code)
);
CREATE INDEX knowledge_item_projects_project_idx
    ON public.knowledge_item_projects (project_code, item_code);

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
