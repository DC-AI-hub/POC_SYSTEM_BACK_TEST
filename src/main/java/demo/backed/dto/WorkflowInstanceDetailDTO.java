package demo.backed.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流实例详情DTO
 * 用于封装工作流实例的详细信息
 */
@Data
public class WorkflowInstanceDetailDTO {
    /**
     * 工作流实例ID
     */
    private Long instanceId;
    
    /**
     * 标题
     */
    private String title;
    
    /**
     * 申请人姓名
     */
    private String applicantName;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 进度百分比
     */
    private int progress;
    
    /**
     * 工作流模板名称
     */
    private String workflowTemplateName;
    
    /**
     * 步骤详情列表
     */
    private List<StepDetail> steps;
    
    /**
     * 步骤详情内部类
     */
    @Data
    public static class StepDetail {
        /**
         * 任务ID
         */
        private String taskId;
        
        /**
         * 任务名称
         */
        private String taskName;
        
        /**
         * 状态
         */
        private String status;
        
        /**
         * 审批人ID
         */
        private Long assigneeId;
        
        /**
         * 审批人姓名
         */
        private String assigneeName;
        
        /**
         * 开始时间
         */
        private LocalDateTime startTime;
        
        /**
         * 结束时间
         */
        private LocalDateTime endTime;
        
        /**
         * 审批意见
         */
        private String comment;
        
        /**
         * 描述
         */
        private String description;
    }
} 