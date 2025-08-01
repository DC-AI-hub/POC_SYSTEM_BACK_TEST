package demo.backed.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

/**
 * 创建费用申请请求DTO
 */
@Data
@ApiModel(description = "创建费用申请请求")
public class CreateExpenseApplicationDTO {
    
    @NotNull(message = "申请人ID不能为空")
    @ApiModelProperty(value = "申请人ID", required = true, example = "1")
    private Long applicantId;
    
    @NotBlank(message = "公司名称不能为空")
    @Size(max = 100, message = "公司名称长度不能超过100字符")
    @ApiModelProperty(value = "费用所属公司", required = true, example = "港交所科技有限公司")
    private String company;
    
    @NotNull(message = "申请日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "申请日期", required = true, example = "2025-06-27")
    private LocalDate applyDate;
    
    @NotBlank(message = "事由描述不能为空")
    @Size(min = 10, max = 500, message = "事由描述长度应在10-500字符之间")
    @ApiModelProperty(value = "事由描述", required = true, example = "参加北京技术交流会议相关费用")
    private String description;
    
    @Pattern(regexp = "CNY|USD|EUR|HKD|JPY|GBP", message = "不支持的币种")
    @ApiModelProperty(value = "币种", example = "CNY", allowableValues = "CNY,USD,EUR,HKD,JPY,GBP")
    private String currency = "CNY";
    
    @Valid
    @NotEmpty(message = "费用明细不能为空")
    @Size(min = 1, max = 20, message = "费用明细数量应在1-20条之间")
    @ApiModelProperty(value = "费用明细列表", required = true)
    private List<ExpenseItemDTO> items;
    
    @Size(max = 500, message = "备注长度不能超过500字符")
    @ApiModelProperty(value = "备注信息")
    private String remark;
    
    // 业务验证方法
    
    /**
     * 验证申请数据完整性
     */
    public void validate() {
        // 验证申请日期不能是未来日期
        if (applyDate != null && applyDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("申请日期不能是未来日期");
        }
        
        // 验证费用明细
        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                ExpenseItemDTO item = items.get(i);
                try {
                    item.validate();
                    // 设置排序号
                    if (item.getSortOrder() == null) {
                        item.setSortOrder(i);
                    }
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(String.format("第%d条明细：%s", i + 1, e.getMessage()));
                }
            }
        }
        
        // 验证费用总金额不超过限制
        double totalAmount = items.stream()
                .mapToDouble(item -> item.getAmount() != null ? item.getAmount().doubleValue() : 0.0)
                .sum();
        
        if (totalAmount > 100000.00) {
            throw new IllegalArgumentException("单次申请总金额不能超过100,000.00");
        }
        
        if (totalAmount <= 0) {
            throw new IllegalArgumentException("申请总金额必须大于0");
        }
    }
    
    /**
     * 获取申请总金额
     */
    public double getTotalAmount() {
        if (items == null || items.isEmpty()) {
            return 0.0;
        }
        return items.stream()
                .mapToDouble(item -> item.getAmount() != null ? item.getAmount().doubleValue() : 0.0)
                .sum();
    }
    
    /**
     * 获取需要发票的明细数量
     */
    public long getReceiptRequiredCount() {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        return items.stream()
                .filter(item -> item.getReceiptRequired() != null && item.getReceiptRequired())
                .count();
    }
    
    /**
     * 检查是否有需要发票的明细
     */
    public boolean hasReceiptRequired() {
        return getReceiptRequiredCount() > 0;
    }
} 