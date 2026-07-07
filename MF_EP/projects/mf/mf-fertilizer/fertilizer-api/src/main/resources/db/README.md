# Database Script Guide

This project does not use Flyway or Liquibase yet. Keep database changes small and explicit.

## Fresh Database

For a new local database, run:

```sql
SOURCE schema.sql;
SOURCE seed-platform-config.sql;
SOURCE seed-faq.sql;
SOURCE seed-activity.sql;
SOURCE init-products.sql;
```

Run merchant seed scripts only when the merchant demo data is needed:

```sql
SOURCE merchant-v1.sql;
SOURCE seed-cyz-merchant-products.sql;
```

## Existing Database

For an existing database, do not rerun `schema.sql` because it drops and recreates tables.

Apply incremental scripts in this order:

```sql
SOURCE article-images-v1.sql;
SOURCE merchant-v1.sql;
```

Then run repeatable seed scripts as needed:

```sql
SOURCE seed-platform-config.sql;
SOURCE seed-faq.sql;
SOURCE seed-activity.sql;
SOURCE init-products.sql;
SOURCE seed-cyz-merchant-products.sql;
```

## Current Required Migration

`article-images-v1.sql` adds `encyclopedia_article.images`, which is required by the article entity and article API. If this script is not applied to an old database, article queries can fail with:

```text
Unknown column 'images' in 'field list'
```

