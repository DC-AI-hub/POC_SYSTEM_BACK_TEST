-- 添加 Keycloak ID 字段到用户表
-- 用于 Keycloak SSO 集成，存储用户在 Keycloak 中的唯一标识符

-- 添加 keycloak_id 字段
ALTER TABLE t_poc_users 
ADD COLUMN keycloak_id VARCHAR(50);

-- 创建索引以提高查询性能
CREATE INDEX idx_users_keycloak_id ON t_poc_users(keycloak_id);

-- 添加注释说明
COMMENT ON COLUMN t_poc_users.keycloak_id IS '用户在 Keycloak 中的唯一标识符，用于 SSO 集成'; 