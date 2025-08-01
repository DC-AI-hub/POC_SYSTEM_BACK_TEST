-- 为工作流模板表添加配置数据字段
-- 解决description字段长度限制问题，避免工作流配置数据截断

-- 添加config_data字段用于存储工作流配置JSON数据（如果不存在）
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 't_poc_workflow_templates' 
                   AND column_name = 'config_data') THEN
        ALTER TABLE t_poc_workflow_templates 
        ADD COLUMN config_data TEXT;
    END IF;
END $$;

-- 将现有description中的配置数据迁移到config_data字段
UPDATE t_poc_workflow_templates 
SET config_data = SUBSTR(description, 9)  -- 移除 '[CONFIG]' 前缀
WHERE description LIKE '[CONFIG]%' AND (config_data IS NULL OR config_data = '');

-- 清理description字段中的配置数据，恢复原有描述功能
UPDATE t_poc_workflow_templates 
SET description = CASE 
    WHEN description LIKE '[CONFIG]%' THEN '流程配置: ' || name
    ELSE description 
END
WHERE description LIKE '[CONFIG]%';

-- 添加注释说明（如果列存在）
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 't_poc_workflow_templates' 
               AND column_name = 'config_data') THEN
        EXECUTE 'COMMENT ON COLUMN t_poc_workflow_templates.config_data IS ''工作流配置数据JSON格式，包含步骤定义、条件分支、审批人配置等详细信息''';
    END IF;
END $$;

COMMENT ON COLUMN t_poc_workflow_templates.description IS '工作流模板的简短描述，用于用户理解模板用途'; 