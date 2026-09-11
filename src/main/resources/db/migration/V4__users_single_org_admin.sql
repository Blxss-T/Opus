-- Pilot constraint: at most one ORG_ADMIN per organization.
CREATE UNIQUE INDEX uk_users_one_org_admin
    ON users (organization_id)
    WHERE role = 'ORG_ADMIN';
