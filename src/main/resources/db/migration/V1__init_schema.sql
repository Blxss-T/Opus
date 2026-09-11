-- Opus initial schema baseline.
-- Enable UUID generation used by later migrations.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
