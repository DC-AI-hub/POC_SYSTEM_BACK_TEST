package demo.backed.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 工作流历史记录DTO
 * 用于表示工作流的历史操作记录
 */
@Data
public class WorkflowHistoryDTO {
    /**
     * 历史记录ID
     */
    private String id;
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 操作类型
     */
    private String operationType; // COMPLETE, REJECT, RETURN, CLAIM
    
    /**
     * 操作人ID
     */
    private Long operatorId;
    
    /**
     * 操作人姓名
     */
    private String operatorName;
    
    /**
     * 操作时间
     */
    private LocalDateTime operationTime;
    
    /**
     * 处理时长（分钟）
     */
    private Integer duration;
    
    /**
     * 操作意见
     */
    private String comment;
    
    /**
     * 任务开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 任务结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 删除原因（如果被删除）
     */
    private String deleteReason;
} 