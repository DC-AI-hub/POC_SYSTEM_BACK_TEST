package demo.backed.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class StartProcessRequest {
    @NotBlank(message = "业务类型不能为空")
    private String businessType;
    
    @NotBlank(message = "业务ID不能为空")
    private String businessId;
    
    @NotNull(message = "申请人ID不能为空")
    private Long applicantId;
    
    private String title;
    
    private BigDecimal amount;
    
    private Map<String, Object> variables;
} 