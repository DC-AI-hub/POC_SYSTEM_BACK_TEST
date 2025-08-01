package demo.backed.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 工作流步骤DTO
 * 用于表示工作流的每个审批步骤
 */
@Data
public class WorkflowStepDTO {
    /**
     * 步骤ID
     */
    private String id;
    
    /**
     * 步骤名称
     */
    private String name;
    
    /**
     * 步骤状态
     */
    private String status; // pending, in-progress, completed, rejected, returned
    
    /**
     * 审批人
     */
    private String approver;
    
    /**
     * 审批时间
     */
    private LocalDateTime approvedAt;
    
    /**
     * 审批意见
     */
    private String comment;
    
    /**
     * 图标名称
     */
    private String icon;
    
    /**
     * 步骤描述
     */
    private String description;
    
    /**
     * 审批人ID
     */
    private Long assigneeId;
    
    /**
     * 处理时长（分钟）
     */
    private Integer duration;
} 