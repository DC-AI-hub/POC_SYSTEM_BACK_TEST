package demo.backed.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ApprovalRequest {
    @NotNull(message = "任务ID不能为空")
    private Long taskId;
    
    @NotBlank(message = "审批意见不能为空")
    private String comment;
    
    private String action; // approve, reject, return
    
    private String targetNodeKey; // 用于打回操作
} 