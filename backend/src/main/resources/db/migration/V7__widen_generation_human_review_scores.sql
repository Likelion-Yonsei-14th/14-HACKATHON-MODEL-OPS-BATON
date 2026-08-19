-- V5 declared these as SMALLINT but the JPA entity maps Integer -> INTEGER; widen to match
-- rather than editing the already-applied V5 (this DB is shared with production BATON).
ALTER TABLE generation_human_reviews ALTER COLUMN coverage_score TYPE INTEGER;
ALTER TABLE generation_human_reviews ALTER COLUMN separation_score TYPE INTEGER;
ALTER TABLE generation_human_reviews ALTER COLUMN granularity_score TYPE INTEGER;
ALTER TABLE generation_human_reviews ALTER COLUMN predecidability_score TYPE INTEGER;
ALTER TABLE generation_human_reviews ALTER COLUMN naturalness_score TYPE INTEGER;
ALTER TABLE generation_human_reviews ALTER COLUMN safety_score TYPE INTEGER;
ALTER TABLE generation_human_reviews ALTER COLUMN overall_score TYPE INTEGER;
