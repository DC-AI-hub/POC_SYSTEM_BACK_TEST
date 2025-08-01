package demo.backed.dto;

import lombok.Data;
import java.util.List;

/**
 * 批量审批请求DTO
 */
@Data
public class BatchApprovalRequest {
    /**
     * 批量审批项目列表
     */
    private List<BatchApprovalItem> items;
    
    /**
     * 批量审批项目
     */
    @Data
    public static class BatchApprovalItem {
        /**
         * 任务ID
         */
        private Long taskId;
        
        /**
         * 操作类型：approve(通过) 或 reject(拒绝)
         */
        private String action;
        
        /**
         * 审批意见
         */
        private String comment;
    }
} 