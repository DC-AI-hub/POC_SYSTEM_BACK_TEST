package demo.backed.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流追踪DTO
 * 用于前端工作流追踪页面显示
 */
@Data
public class WorkflowTrackerDTO {
    /**
     * 工作流实例ID
     */
    private String id;
    
    /**
     * 申请标题
     */
    private String title;
    
    /**
     * 申请人
     */
    private String applicant;
    
    /**
     * 申请时间
     */
    private LocalDateTime applicationDate;
    
    /**
     * 总体状态
     */
    private String status; // draft, in-progress, completed, rejected
    
    /**
     * 当前步骤索引
     */
    private int currentStep;
    
    /**
     * 进度百分比
     */
    private int progress;
    
    /**
     * 步骤列表
     */
    private List<WorkflowStepDTO> steps;
    
    /**
     * 业务类型
     */
    private String businessType;
    
    /**
     * 业务ID
     */
    private String businessId;
} 