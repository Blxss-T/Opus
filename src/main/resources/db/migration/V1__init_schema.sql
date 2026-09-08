-- OpsFlow Initial Database Schema Baseline
-- Migration V1: Establish uuid-ossp extension for UUID generation

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Schema baseline verification comment
COMMENT ON DATABASE current_database() IS 'OpsFlow Operations Management Platform Database';
