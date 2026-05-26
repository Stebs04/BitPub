CREATE TABLE permissions (
    id UUID PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE roles (
    id UUID PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id),
    permission_id UUID NOT NULL REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    token_version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id),
    role_id UUID NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE user_locales (
    user_id UUID NOT NULL REFERENCES users(id),
    locale_id VARCHAR(50) NOT NULL
);

-- Insert default admin user and roles
INSERT INTO permissions (id, name) VALUES ('b295f725-b829-4d64-a690-31612ee4df11', 'MANAGE_USERS');
INSERT INTO permissions (id, name) VALUES ('b295f725-b829-4d64-a690-31612ee4df12', 'MANAGE_GAMES');

INSERT INTO roles (id, name) VALUES ('c195f725-b829-4d64-a690-31612ee4df11', 'ADMIN');
INSERT INTO roles (id, name) VALUES ('c195f725-b829-4d64-a690-31612ee4df12', 'USER');

INSERT INTO role_permissions (role_id, permission_id) VALUES ('c195f725-b829-4d64-a690-31612ee4df11', 'b295f725-b829-4d64-a690-31612ee4df11');
INSERT INTO role_permissions (role_id, permission_id) VALUES ('c195f725-b829-4d64-a690-31612ee4df11', 'b295f725-b829-4d64-a690-31612ee4df12');

-- password is "admin"
INSERT INTO users (id, username, password, email, token_version) 
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'admin', '$2a$10$t3Xy7lV2wO3T6v9b2eLzZOMvB5HkH8H5XG8Z.0Y/R6YyW.JcM1q/C', 'admin@bitpub.com', 0);

INSERT INTO user_roles (user_id, role_id) VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'c195f725-b829-4d64-a690-31612ee4df11');
