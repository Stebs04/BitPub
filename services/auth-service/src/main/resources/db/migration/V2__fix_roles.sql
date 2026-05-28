-- V2: Fix roles to match com.bitpub.common.security.enums.Role
-- Remove old incorrect roles (cascades via user_roles, role_permissions)

DELETE FROM user_roles;
DELETE FROM role_permissions;
DELETE FROM roles;

-- Insert correct roles matching the Role enum
INSERT INTO roles (id, name) VALUES ('d195f725-b829-4d64-a690-31612ee4df01', 'PLAYER');
INSERT INTO roles (id, name) VALUES ('d195f725-b829-4d64-a690-31612ee4df02', 'LOCAL_ADMIN');
INSERT INTO roles (id, name) VALUES ('d195f725-b829-4d64-a690-31612ee4df03', 'GAME_ADMIN');
INSERT INTO roles (id, name) VALUES ('d195f725-b829-4d64-a690-31612ee4df04', 'PLATFORM_ADMIN');

-- Re-assign admin user to PLATFORM_ADMIN
INSERT INTO user_roles (user_id, role_id)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'd195f725-b829-4d64-a690-31612ee4df04');

-- Assign permissions to PLATFORM_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
VALUES ('d195f725-b829-4d64-a690-31612ee4df04', 'b295f725-b829-4d64-a690-31612ee4df11');
INSERT INTO role_permissions (role_id, permission_id)
VALUES ('d195f725-b829-4d64-a690-31612ee4df04', 'b295f725-b829-4d64-a690-31612ee4df12');
