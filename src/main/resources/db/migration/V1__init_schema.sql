-- OpsFlow Initial Database Schema Baseline
-- Migration V1: Establish uuid-ossp extension for UUID generation

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Schema baseline verification comment
COMMENT ON DATABASE opus_db IS 'OpsFlow Operations Management Platform Database';
