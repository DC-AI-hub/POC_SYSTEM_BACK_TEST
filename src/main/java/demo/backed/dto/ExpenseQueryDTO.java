package demo.backed.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import demo.backed.entity.ApplicationStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDate;

/**
 * 费用申请查询条件DTO
 */
@Data
@ApiModel(description = "费用申请查询条件")
public class ExpenseQueryDTO {
    
    @ApiModelProperty(value = "申请人ID")
    private Long applicantId;
    
    @ApiModelProperty(value = "申请人姓名")
    private String applicantName;
    
    @ApiModelProperty(value = "部门")
    private String department;
    
    @ApiModelProperty(value = "申请编号")
    private String applicationNumber;
    
    @ApiModelProperty(value = "申请状态")
    private ApplicationStatus status;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "开始日期")
    private LocalDate startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "结束日期")
    private LocalDate endDate;
    
    @ApiModelProperty(value = "币种")
    private String currency;
    
    @ApiModelProperty(value = "公司")
    private String company;
    
    @ApiModelProperty(value = "最小金额")
    private Double minAmount;
    
    @ApiModelProperty(value = "最大金额")
    private Double maxAmount;
} 