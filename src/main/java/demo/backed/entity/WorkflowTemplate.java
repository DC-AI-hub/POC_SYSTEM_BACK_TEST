package demo.backed.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 工作流模板实体
 */
@Entity
@Table(name = "t_poc_workflow_templates")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkflowTemplate extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 模板名称
     */
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    
    /**
     * 模板描述
     */
    @Column(name = "description", length = 500)
    private String description;
    
    /**
     * 流程定义KEY（用于Flowable）
     */
    @Column(name = "process_key", unique = true, nullable = false, length = 100)
    private String processKey;
    
    /**
     * 模板类型（expense: 费用审批, approval: 审批流程, custom: 自定义）
     */
    @Column(name = "type", length = 50)
    private String type;
    
    /**
     * 状态（active: 已启用, draft: 草稿, archived: 已归档）
     */
    @Column(name = "status", length = 20)
    private String status = "draft";
    
    /**
     * BPMN XML内容
     */
    @Column(name = "bpmn_xml", columnDefinition = "TEXT")
    private String bpmnXml;
    
    /**
     * 工作流配置数据（JSON格式，包含步骤、分支等详细配置）
     */
    @Column(name = "config_data", columnDefinition = "TEXT")
    private String configData;
    
    /**
     * Flowable部署ID
     */
    @Column(name = "deployment_id", length = 100)
    private String deploymentId;
    
    /**
     * 模板版本号
     */
    @Column(name = "template_version")
    private Integer templateVersion = 1;
    
    /**
     * 是否已部署到Flowable
     */
    @Column(name = "is_deployed")
    private Boolean isDeployed = false;
    
    /**
     * 部署时间
     */
    @Column(name = "deployed_time")
    private LocalDateTime deployedTime;
} 