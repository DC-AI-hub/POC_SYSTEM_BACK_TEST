package demo.backed.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 费用明细DTO
 */
@Data
@ApiModel(description = "费用明细数据传输对象")
public class ExpenseItemDTO {
    
    @ApiModelProperty(value = "明细ID")
    private Long id;
    
    @ApiModelProperty(value = "申请单ID")
    private Long applicationId;
    
    @NotBlank(message = "费用科目不能为空")
    @Size(max = 100, message = "费用科目长度不能超过100字符")
    @ApiModelProperty(value = "费用科目", example = "交通费", required = true)
    private String expenseCategory;
    
    @NotBlank(message = "用途说明不能为空")
    @Size(max = 200, message = "用途说明长度不能超过200字符")
    @ApiModelProperty(value = "用途说明", example = "北京出差高铁票", required = true)
    private String purpose;
    
    @NotNull(message = "费用金额不能为空")
    @DecimalMin(value = "0.01", message = "费用金额必须大于0")
    @DecimalMax(value = "999999.99", message = "单项费用不能超过999999.99")
    @Digits(integer = 6, fraction = 2, message = "金额格式不正确")
    @ApiModelProperty(value = "费用金额", example = "800.00", required = true)
    private BigDecimal amount;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "费用发生日期", example = "2025-06-27")
    private LocalDate expenseDate;
    
    @Size(max = 500, message = "备注长度不能超过500字符")
    @ApiModelProperty(value = "备注信息")
    private String remark;
    
    @ApiModelProperty(value = "排序号")
    private Integer sortOrder;
    
    @ApiModelProperty(value = "是否需要发票", example = "true")
    private Boolean receiptRequired = true;
    
    // 业务方法
    
    /**
     * 检查是否需要发票
     */
    public boolean isReceiptRequired() {
        return receiptRequired != null && receiptRequired;
    }
    
    /**
     * 验证明细数据完整性
     */
    public void validate() {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("费用金额必须大于0");
        }
        
        if (expenseCategory == null || expenseCategory.trim().isEmpty()) {
            throw new IllegalArgumentException("费用科目不能为空");
        }
        
        if (purpose == null || purpose.trim().isEmpty()) {
            throw new IllegalArgumentException("用途说明不能为空");
        }
        
        // 验证金额精度
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("金额最多保留两位小数");
        }
    }
    

} 