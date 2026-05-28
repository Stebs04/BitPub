-- V4: Seed test users for each role and wipe old users

-- 1. Wipe existing users and their relationships
DELETE FROM user_locales;
DELETE FROM user_roles;
DELETE FROM users;

-- 2. Insert test users
-- The BCrypt hash is for the password: "admin"
-- $2a$10$cC2M1lrQYieguPygCQHw1OcPhvuB12RVoHyrUflkQ7jRtGoZVn5z2

-- User: player
INSERT INTO users (id, username, password, email, token_version) 
VALUES ('c295f725-b829-4d64-a690-31612ee4df01', 'player_user', '$2a$10$cC2M1lrQYieguPygCQHw1OcPhvuB12RVoHyrUflkQ7jRtGoZVn5z2', 'player@bitpub.com', 0);

-- User: local admin
INSERT INTO users (id, username, password, email, token_version) 
VALUES ('c295f725-b829-4d64-a690-31612ee4df02', 'local_admin_user', '$2a$10$cC2M1lrQYieguPygCQHw1OcPhvuB12RVoHyrUflkQ7jRtGoZVn5z2', 'local@bitpub.com', 0);

-- User: game admin
INSERT INTO users (id, username, password, email, token_version) 
VALUES ('c295f725-b829-4d64-a690-31612ee4df03', 'game_admin_user', '$2a$10$cC2M1lrQYieguPygCQHw1OcPhvuB12RVoHyrUflkQ7jRtGoZVn5z2', 'game@bitpub.com', 0);

-- User: platform admin
INSERT INTO users (id, username, password, email, token_version) 
VALUES ('c295f725-b829-4d64-a690-31612ee4df04', 'platform_admin_user', '$2a$10$cC2M1lrQYieguPygCQHw1OcPhvuB12RVoHyrUflkQ7jRtGoZVn5z2', 'platform@bitpub.com', 0);

-- 3. Assign roles
-- PLAYER
INSERT INTO user_roles (user_id, role_id) 
VALUES ('c295f725-b829-4d64-a690-31612ee4df01', 'd195f725-b829-4d64-a690-31612ee4df01');

-- LOCAL_ADMIN
INSERT INTO user_roles (user_id, role_id) 
VALUES ('c295f725-b829-4d64-a690-31612ee4df02', 'd195f725-b829-4d64-a690-31612ee4df02');

-- GAME_ADMIN
INSERT INTO user_roles (user_id, role_id) 
VALUES ('c295f725-b829-4d64-a690-31612ee4df03', 'd195f725-b829-4d64-a690-31612ee4df03');

-- PLATFORM_ADMIN
INSERT INTO user_roles (user_id, role_id) 
VALUES ('c295f725-b829-4d64-a690-31612ee4df04', 'd195f725-b829-4d64-a690-31612ee4df04');

-- 4. Assign a fake locale ID to the Local Admin (required for the dashboard)
INSERT INTO user_locales (user_id, locale_id)
VALUES ('c295f725-b829-4d64-a690-31612ee4df02', '550e8400-e29b-41d4-a716-446655440000');
