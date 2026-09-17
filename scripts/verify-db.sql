\set ON_ERROR_STOP on
SELECT extname, extversion FROM pg_extension WHERE extname IN ('age', 'pgroonga');
SELECT kind, count(*) FROM public.knowledge_items GROUP BY kind ORDER BY kind;
SELECT m.subject, count(DISTINCT access.item_code) AS visible_items
FROM public.user_project_memberships m
JOIN public.knowledge_item_projects access ON access.project_code = m.project_code
GROUP BY m.subject ORDER BY m.subject;
SELECT m.subject, array_agg(DISTINCT i.code ORDER BY i.code) AS joint_delivery_hits
FROM public.user_project_memberships m
JOIN public.knowledge_item_projects access ON access.project_code = m.project_code
JOIN public.knowledge_items i ON i.code = access.item_code
WHERE i.title &@ '共同配送' OR i.content &@ '共同配送' OR i.aliases &@ '共同配送'
GROUP BY m.subject ORDER BY m.subject;
SELECT code, title FROM public.knowledge_items
WHERE kind = 'RULE' AND region IN ('北海道', '全国') AND status = 'APPROVED';
SELECT code, title FROM public.knowledge_items
WHERE title &@ '冷蔵' OR content &@ '冷蔵' OR aliases &@ '冷蔵';
WITH RECURSIVE related AS (
  SELECT 1 AS depth, target_code AS code FROM public.knowledge_relations WHERE source_code = 'NEED-MARGIN'
  UNION ALL
  SELECT r.depth + 1, l.target_code
  FROM related r JOIN public.knowledge_relations l ON l.source_code = r.code
  WHERE r.depth < 2
)
SELECT depth, code FROM related ORDER BY depth, code;
LOAD 'age';
SET search_path = ag_catalog, public;
SELECT * FROM cypher('business_knowledge', $$
  MATCH (need:Knowledge {code: 'NEED-MARGIN'})-[relation]->(related:Knowledge)
  RETURN type(relation), related.code
$$) AS (relation agtype, related_code agtype);
