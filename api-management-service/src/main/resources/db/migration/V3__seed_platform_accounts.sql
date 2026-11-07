-- Create platform admin user
INSERT INTO management_users (
    organization_id,
    role_id,
    username,
    password_hash,
    enabled,
    created_at,
    updated_at
)
SELECT
    NULL,
    r.id,
    'platform-admin',
    '$2a$12$olKjin9T2ph8WXP25VNaJ.L3irXTFkMIfCKIz1BSRyBthVgTYvKzq',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM roles r
WHERE r.name = 'PLATFORM_ADMIN';


-- Create gateway service user
INSERT INTO management_users (
    organization_id,
    role_id,
    username,
    password_hash,
    enabled,
    created_at,
    updated_at
)
SELECT
    NULL,
    r.id,
    'gateway-service',
    '$2a$12$kJc4nbK7ZkDN0iPLxJMeDOrkR3bHePTZ3h18nwWk.DFZqw2xVJdDu',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM roles r
WHERE r.name = 'GATEWAY_SERVICE';