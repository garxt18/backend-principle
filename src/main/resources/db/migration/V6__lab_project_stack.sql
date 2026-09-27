-- V6: the Rebuild Lab understands more than Spring Boot. Each project remembers its stack so layer names
-- and explanations fit it (e.g. "API route handlers" for Next.js instead of "Controllers").
ALTER TABLE lab_projects ADD COLUMN stack VARCHAR(20) NOT NULL DEFAULT 'SPRING';
