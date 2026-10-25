-- Run from the repository root after creating an empty PostgreSQL database:
--   psql "$env:TRUSTVAULT_DATABASE_URL" -v ON_ERROR_STOP=1 -f trustvault/database/setup.sql
\set ON_ERROR_STOP on
\ir schema.sql
