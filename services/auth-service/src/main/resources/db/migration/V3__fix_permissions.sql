-- V3: Fix permissions to match com.bitpub.common.security.enums.Permission

DELETE FROM role_permissions;
DELETE FROM permissions;

-- Insert correct permissions matching the Permission enum
INSERT INTO permissions (id, name) VALUES ('e295f725-b829-4d64-a690-31612ee4df01', 'MATCH_READ_SELF');
INSERT INTO permissions (id, name) VALUES ('e295f725-b829-4d64-a690-31612ee4df02', 'STATS_READ_SELF');
INSERT INTO permissions (id, name) VALUES ('e295f725-b829-4d64-a690-31612ee4df03', 'TOURNAMENT_JOIN');
INSERT INTO permissions (id, name) VALUES ('e295f725-b829-4d64-a690-31612ee4df04', 'PROFILE_UPDATE_SELF');
INSERT INTO permissions (id, name) VALUES ('e295f725-b829-4d64-a690-31612ee4df05', 'GAME_MANAGE_LOCAL');
INSERT INTO permissions (id, name) VALUES ('e295f725-b829-4d64-a690-31612ee4df06', 'MATCH_MANAGE_LOCAL');
INSERT INTO permissions (id, name) VALUES ('e295f725-b829-4d64-a690-31612ee4df07', 'EDGE_MANAGE_LOCAL');
INSERT INTO permissions (id, name) VALUES ('e295f725-b829-4d64-a690-31612ee4df08', 'USERS_READ_LOCAL');
INSERT INTO permissions (id, name) VALUES ('e295f725-b829-4d64-a690-31612ee4df09', 'STATS_READ_LOCAL');
INSERT INTO permissions (id, name) VALUES ('e295f725-b829-4d64-a690-31612ee4df10', 'GAME_TYPE_CREATE');
INSERT INTO permissions (id, name) VALUES ('e295f725-b829-4d64-a690-31612ee4df11', 'GAME_TYPE_UPDATE');
INSERT INTO permissions (id, name) VALUES ('e295f725-b829-4d64-a690-31612ee4df12', 'SENSOR_CONFIG_MANAGE');
INSERT INTO permissions (id, name) VALUES ('e295f725-b829-4d64-a690-31612ee4df13', 'TOURNAMENT_MANAGE_GLOBAL');
INSERT INTO permissions (id, name) VALUES ('e295f725-b829-4d64-a690-31612ee4df14', 'USERS_MANAGE_GLOBAL');
INSERT INTO permissions (id, name) VALUES ('e295f725-b829-4d64-a690-31612ee4df15', 'SYSTEM_CONFIG');
INSERT INTO permissions (id, name) VALUES ('e295f725-b829-4d64-a690-31612ee4df16', 'GLOBAL_STATS_READ');
INSERT INTO permissions (id, name) VALUES ('e295f725-b829-4d64-a690-31612ee4df17', 'ROLE_MANAGE');
INSERT INTO permissions (id, name) VALUES ('e295f725-b829-4d64-a690-31612ee4df18', 'PLATFORM_MONITORING');

-- Assign permissions to PLATFORM_ADMIN (d195f725-b829-4d64-a690-31612ee4df04)
INSERT INTO role_permissions (role_id, permission_id)
VALUES ('d195f725-b829-4d64-a690-31612ee4df04', 'e295f725-b829-4d64-a690-31612ee4df14'); -- USERS_MANAGE_GLOBAL
INSERT INTO role_permissions (role_id, permission_id)
VALUES ('d195f725-b829-4d64-a690-31612ee4df04', 'e295f725-b829-4d64-a690-31612ee4df15'); -- SYSTEM_CONFIG
