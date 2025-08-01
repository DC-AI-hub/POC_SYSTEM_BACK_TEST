package demo.backed.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import demo.backed.entity.ApplicationStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 费用申请详情响应DTO
 */
@Data
@ApiModel(description = "费用申请详情")
public class ExpenseApplicationDTO {
    
    @ApiModelProperty(value = "申请ID")
    private Long id;
    
    @ApiModelProperty(value = "申请编号", example = "EXP-2025-000001")
    private String applicationNumber;
    
    @ApiModelProperty(value = "申请人ID")
    private Long applicantId;
    
    @ApiModelProperty(value = "申请人姓名")
    private String applicantName;
    
    @ApiModelProperty(value = "申请人部门")
    private String department;
    
    @ApiModelProperty(value = "费用所属公司")
    private String company;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "申请日期")
    private LocalDate applyDate;
    
    @ApiModelProperty(value = "事由描述")
    private String description;
    
    @ApiModelProperty(value = "申请总金额")
    private BigDecimal totalAmount;
    
    @ApiModelProperty(value = "币种", example = "CNY")
    private String currency;
    
    @ApiModelProperty(value = "申请状态")
    private ApplicationStatus status;
    
    @ApiModelProperty(value = "状态描述")
    private String statusDescription;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @ApiModelProperty(value = "提交时间")
    private LocalDateTime submitTime;
    
    @ApiModelProperty(value = "工作流实例ID")
    private String workflowInstanceId;
    
    @ApiModelProperty(value = "费用明细列表")
    private List<ExpenseItemDTO> items;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedTime;
    
    @ApiModelProperty(value = "创建人")
    private String createdBy;
    
    @ApiModelProperty(value = "更新人")
    private String updatedBy;
    
    // 扩展字段
    
    @ApiModelProperty(value = "明细数量")
    private Integer itemCount;
    
    @ApiModelProperty(value = "是否可编辑")
    private Boolean canEdit;
    
    @ApiModelProperty(value = "是否可提交审批")
    private Boolean canSubmit;
    
    @ApiModelProperty(value = "是否已完结")
    private Boolean isFinal;
    
    @ApiModelProperty(value = "需要发票的明细数量")
    private Long receiptRequiredCount;
    
    // 业务方法
    
    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        return status != null ? status.getDescription() : "未知状态";
    }
    
    /**
     * 获取明细数量
     */
    public Integer getItemCount() {
        return items != null ? items.size() : 0;
    }
    
    /**
     * 检查是否可以编辑
     */
    public Boolean getCanEdit() {
        return status != null && status.canEdit();
    }
    
    /**
     * 检查是否可以提交审批
     */
    public Boolean getCanSubmit() {
        return status != null && status.canSubmitForApproval();
    }
    
    /**
     * 检查是否已完结
     */
    public Boolean getIsFinal() {
        return status != null && status.isFinalStatus();
    }
    
    /**
     * 获取需要发票的明细数量
     */
    public Long getReceiptRequiredCount() {
        if (items == null || items.isEmpty()) {
            return 0L;
        }
        return items.stream()
                .filter(item -> item.getReceiptRequired() != null && item.getReceiptRequired())
                .count();
    }
    
    /**
     * 格式化显示总金额
     */
    public String getFormattedTotalAmount() {
        if (totalAmount == null) {
            return "0.00";
        }
        return String.format("%.2f", totalAmount);
    }
    
    /**
     * 获取申请摘要信息
     */
    public String getSummary() {
        return String.format("申请编号：%s，金额：%s %s，状态：%s", 
                applicationNumber, 
                getFormattedTotalAmount(), 
                currency != null ? currency : "CNY", 
                getStatusDescription());
    }
    
    /**
     * 验证数据完整性
     */
    public boolean isValid() {
        return applicationNumber != null && 
               applicantId != null && 
               totalAmount != null && 
               totalAmount.compareTo(BigDecimal.ZERO) > 0 &&
               status != null &&
               items != null && 
               !items.isEmpty();
    }
} 