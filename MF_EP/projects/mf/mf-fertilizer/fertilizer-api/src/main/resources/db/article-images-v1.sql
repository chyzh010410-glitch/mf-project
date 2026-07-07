-- v1: Add multi-image support for encyclopedia articles.
-- Safe to run more than once on MySQL 8.x.

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'encyclopedia_article'
      AND column_name = 'images'
);

SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE encyclopedia_article ADD COLUMN images TEXT DEFAULT NULL COMMENT ''图片JSON数组'' AFTER cover_image',
    'SELECT ''encyclopedia_article.images already exists'''
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
