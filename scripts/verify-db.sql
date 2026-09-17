\set ON_ERROR_STOP on
SELECT extname, extversion FROM pg_extension WHERE extname IN ('age', 'pgroonga');
SELECT id, title FROM public.business_rules WHERE category = '配送' AND region = '北海道';
SELECT id, title FROM public.business_rules WHERE content &@ '冷蔵';
LOAD 'age';
SET search_path = ag_catalog, public;
SELECT * FROM cypher('transport_rules', $$
  MATCH (r:Rule)-[:EXTENDS]->(base:Rule)
  RETURN r.rule_id, base.rule_id
$$) AS (rule_id agtype, base_rule_id agtype);
