package demo.backed.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 费用明细表实体
 */
@Entity
@Table(name = "t_poc_expense_items")
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "费用明细")
public class ExpenseItem extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "明细ID")
    private Long id;
    
    @Column(name = "application_id", nullable = false)
    @ApiModelProperty(value = "申请单ID")
    private Long applicationId;
    
    @Column(name = "expense_category", length = 100)
    @ApiModelProperty(value = "费用科目", example = "交通费")
    private String expenseCategory;
    
    @Column(name = "purpose", length = 200)
    @ApiModelProperty(value = "用途说明")
    private String purpose;
    
    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    @ApiModelProperty(value = "费用金额")
    private BigDecimal amount;
    
    @Column(name = "expense_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "费用发生日期")
    private LocalDate expenseDate;
    
    @Column(name = "remark", length = 500)
    @ApiModelProperty(value = "备注")
    private String remark;
    
    @Column(name = "sort_order")
    @ApiModelProperty(value = "排序")
    private Integer sortOrder;
    
    @Column(name = "receipt_required")
    @ApiModelProperty(value = "是否需要发票")
    private Boolean receiptRequired = true;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", insertable = false, updatable = false)
    @ApiModelProperty(value = "关联的申请单", hidden = true)
    private ExpenseApplication application;
    
    // 便捷方法
    
    /**
     * 验证明细数据
     */
    public void validate() {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("费用金额必须大于0");
        }
        
        if (expenseCategory == null || expenseCategory.trim().isEmpty()) {
            throw new RuntimeException("费用科目不能为空");
        }
        
        if (purpose == null || purpose.trim().isEmpty()) {
            throw new RuntimeException("用途说明不能为空");
        }
    }
    
    /**
     * 检查是否需要发票
     */
    public boolean isReceiptRequired() {
        return receiptRequired != null && receiptRequired;
    }
} 