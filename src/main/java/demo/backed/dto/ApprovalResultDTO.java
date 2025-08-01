package demo.backed.dto;

import lombok.Data;

/**
 * 审批结果DTO
 * 用于返回审批操作的结果信息
 */
@Data
public class ApprovalResultDTO {
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 流程实例ID
     */
    private String processInstanceId;
    
    /**
     * 操作是否成功
     */
    private boolean success;
    
    /**
     * 流程是否完成
     */
    private boolean processCompleted;
    
    /**
     * 是否被拒绝
     */
    private boolean rejected;
    
    /**
     * 是否被打回
     */
    private boolean returned;
    
    /**
     * 打回到的节点
     */
    private String returnToNode;
    
    /**
     * 下一步任务名称
     */
    private String nextTaskName;
    
    /**
     * 下一步审批人
     */
    private String nextAssignee;
    
    /**
     * 操作消息
     */
    private String message;
} 