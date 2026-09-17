-- 架空の「青空運送」のデモ規程。実在する会社の規程ではない。
INSERT INTO public.business_rules (id, title, category, region, content, effective_from) VALUES
('R001', '通常配送の受付締切', '配送', '全国', '通常配送は営業日の15時までの受付で翌営業日に出荷する。離島は別途日数が必要。', '2026-01-01'),
('R002', '冷蔵品の温度管理', '温度管理', '全国', '冷蔵品は2度から8度で輸送する。集荷時と引渡時に温度を記録する。', '2026-01-01'),
('R003', '北海道の冬季配送', '配送', '北海道', '12月から3月は通常の配送予定に2営業日を追加する。天候による遅延は荷主へ連絡する。', '2026-01-01');

LOAD 'age';
SET search_path = ag_catalog, public;
SELECT * FROM cypher('transport_rules', $$
  CREATE (standard:Rule {rule_id: 'R001'}),
         (cold:Rule {rule_id: 'R002'}),
         (winter:Rule {rule_id: 'R003'}),
         (winter)-[:EXTENDS]->(standard),
         (cold)-[:RELATED_TO]->(standard)
$$) AS (result agtype);
