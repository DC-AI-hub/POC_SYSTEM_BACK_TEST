package demo.backed.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 费用申请主表实体
 */
@Entity
@Table(name = "t_poc_expense_applications")
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "费用申请")
public class ExpenseApplication extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "申请ID")
    private Long id;
    
    @Column(name = "application_number", unique = true, length = 50)
    @ApiModelProperty(value = "申请编号", example = "EXP-2025-123456")
    private String applicationNumber;
    
    @Column(name = "applicant_id", nullable = false)
    @ApiModelProperty(value = "申请人ID")
    private Long applicantId;
    
    @Column(name = "applicant_name", length = 50)
    @ApiModelProperty(value = "申请人姓名")
    private String applicantName;
    
    @Column(name = "department", length = 100)
    @ApiModelProperty(value = "申请人部门")
    private String department;
    
    @Column(name = "company", length = 100)
    @ApiModelProperty(value = "费用所属公司")
    private String company;
    
    @Column(name = "apply_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "申请日期")
    private LocalDate applyDate;
    
    @Column(name = "description", length = 500)
    @ApiModelProperty(value = "事由描述")
    private String description;
    
    @Column(name = "total_amount", precision = 10, scale = 2)
    @ApiModelProperty(value = "申请总金额")
    private BigDecimal totalAmount;
    
    @Column(name = "currency", length = 10)
    @ApiModelProperty(value = "币种", example = "CNY")
    private String currency = "CNY";
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @ApiModelProperty(value = "申请状态")
    private ApplicationStatus status = ApplicationStatus.DRAFT;
    
    @Column(name = "submit_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @ApiModelProperty(value = "提交时间")
    private LocalDateTime submitTime;
    
    @Column(name = "workflow_instance_id", length = 64)
    @ApiModelProperty(value = "工作流实例ID")
    private String workflowInstanceId;
    
    @OneToMany(mappedBy = "applicationId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ApiModelProperty(value = "费用明细列表")
    private List<ExpenseItem> items = new ArrayList<>();
    
    // 便捷方法
    
    /**
     * 添加费用明细
     */
    public void addItem(ExpenseItem item) {
        item.setApplicationId(this.getId());
        this.items.add(item);
    }
    
    /**
     * 移除费用明细
     */
    public void removeItem(ExpenseItem item) {
        this.items.remove(item);
        item.setApplicationId(null);
    }
    
    /**
     * 清空费用明细
     */
    public void clearItems() {
        this.items.forEach(item -> item.setApplicationId(null));
        this.items.clear();
    }
    
    /**
     * 计算总金额
     */
    public BigDecimal calculateTotalAmount() {
        return items.stream()
                .map(ExpenseItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * 检查是否可以编辑
     */
    public boolean canEdit() {
        return status != null && status.canEdit();
    }
    
    /**
     * 检查是否可以提交审批
     */
    public boolean canSubmitForApproval() {
        return status != null && status.canSubmitForApproval();
    }
    
    /**
     * 提交审批前的验证
     */
    public void validateForSubmission() {
        if (items.isEmpty()) {
            throw new RuntimeException("请至少添加一条费用明细");
        }
        
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("申请金额必须大于0");
        }
        
        if (applicantId == null) {
            throw new RuntimeException("申请人信息不能为空");
        }
        
        if (applyDate == null) {
            throw new RuntimeException("申请日期不能为空");
        }
    }
} 