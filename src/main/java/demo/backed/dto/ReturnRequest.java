package demo.backed.dto;

import lombok.Data;

/**
 * 打回请求DTO
 */
@Data
public class ReturnRequest {
    /**
     * 目标节点Key
     */
    private String targetNodeKey;
    
    /**
     * 打回理由（必填）
     */
    private String comment;
} 