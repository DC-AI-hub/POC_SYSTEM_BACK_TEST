package demo.backed.dto;

import lombok.Data;

/**
 * 可打回节点DTO
 */
@Data
public class ReturnableNodeDTO {
    /**
     * 节点Key
     */
    private String nodeKey;
    
    /**
     * 节点名称
     */
    private String nodeName;
    
    /**
     * 审批人姓名
     */
    private String assigneeName;
    
    /**
     * 审批时间
     */
    private String approvedTime;
} 