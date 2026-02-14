-- Add author tracking to posts.
-- author_id stores the Keycloak user UUID (JWT "sub" claim).
-- Nullable for backward compatibility with existing posts.

ALTER TABLE posts ADD COLUMN author_id VARCHAR(255);
CREATE INDEX idx_posts_author_id ON posts(author_id);
