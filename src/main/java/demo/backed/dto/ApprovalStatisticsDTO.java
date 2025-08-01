package demo.backed.dto;

import lombok.Data;

/**
 * 审批统计信息DTO
 */
@Data
public class ApprovalStatisticsDTO {
    /**
     * 我的待办数量
     */
    private int myPendingCount;
    
    /**
     * 今日已审批数量
     */
    private int myApprovedToday;
    
    /**
     * 并行审批数量
     */
    private int parallelApprovalCount;
    
    /**
     * 委托审批数量
     */
    private int delegatedApprovalCount;
    
    /**
     * 紧急任务数量
     */
    private int urgentTaskCount;
    
    /**
     * 逾期任务数量
     */
    private int overdueTaskCount;
} 