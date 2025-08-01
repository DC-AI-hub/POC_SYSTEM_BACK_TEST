package demo.backed.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingTaskDTO {
    // 核心标识字段
    private String taskId;              // 修改为String，对应前端期望
    private String flowableTaskId;      // Flowable原始任务ID
    private Long instanceId;
    private String processInstanceId;
    
    // 申请信息字段
    private String title;
    private String applicationNumber;   // 新增：申请单号
    private String applicantName;
    private String department;          // 新增：申请人部门
    
    // 时间字段
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date submitTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") 
    private String createTime;          // 新增：对应前端期望的createTime
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dueDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date approvedTime;
    
    // 审批节点信息
    private String taskName;            // 修改字段名：currentNodeName -> taskName
    private String currentNodeName;     // 保留原字段，兼容性
    private String assignee;            // 新增：当前审批人
    
    // 业务数据
    private Double amount;              // 修改为Double，对应前端number类型
    private BigDecimal originalAmount;  // 保留原始BigDecimal字段
    private String businessType;        // EXPENSE | TRAVEL
    private String businessId;
    private String description;
    
    // 状态和优先级
    private String status;              // pending | in-progress | approved | rejected
    private String priority;           // high | medium | low
    
    // 审批相关
    private Boolean delegated;          // 修改字段名：isProxy -> delegated
    private Boolean isProxy;            // 保留原字段，兼容性
    private String comment;
    
    // 扩展字段
    private Integer attachmentCount;    // 新增：附件数量
} 