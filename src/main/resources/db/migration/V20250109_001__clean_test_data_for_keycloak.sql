-- 清理测试数据，为Keycloak用户同步做准备
-- 删除所有测试用户数据，以便重新从Keycloak同步真实用户

-- 首先删除相关的外键约束数据
-- 删除费用申请相关数据
DELETE FROM t_poc_expense_items WHERE application_id IN (
    SELECT id FROM t_poc_expense_applications WHERE applicant_id IN (
        SELECT id FROM t_poc_users WHERE keycloak_id IS NULL
    )
);

DELETE FROM t_poc_expense_applications WHERE applicant_id IN (
    SELECT id FROM t_poc_users WHERE keycloak_id IS NULL
);

-- 删除工作流实例相关数据
DELETE FROM t_poc_workflow_instances WHERE applicant_id IN (
    SELECT id FROM t_poc_users WHERE keycloak_id IS NULL
);

-- 删除附件文件相关数据
DELETE FROM t_poc_attachment_files WHERE upload_user_id IN (
    SELECT id FROM t_poc_users WHERE keycloak_id IS NULL
);

-- 删除所有没有keycloak_id的用户（测试数据）
DELETE FROM t_poc_users WHERE keycloak_id IS NULL;

-- 输出清理结果
-- 这个查询会在迁移后显示剩余的用户数量
SELECT 
    '清理完成' AS status,
    COUNT(*) AS remaining_users,
    COUNT(CASE WHEN keycloak_id IS NOT NULL THEN 1 END) AS keycloak_users,
    COUNT(CASE WHEN keycloak_id IS NULL THEN 1 END) AS test_users
FROM t_poc_users; 