package demo.backed.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_poc_organization_relations", indexes = {
    @Index(name = "idx_org_employee", columnList = "employee_id"),
    @Index(name = "idx_org_department", columnList = "department_id"),
    @Index(name = "idx_org_position", columnList = "position_id"),
    @Index(name = "idx_org_manager", columnList = "manager_id"),
    @Index(name = "idx_org_status", columnList = "status"),
    @Index(name = "idx_org_effective", columnList = "effective_date"),
    @Index(name = "idx_org_type", columnList = "relation_type")
})
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "组织关系实体")
public class OrganizationRelation extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "employee_id", nullable = false)
    @ApiModelProperty(value = "员工ID", required = true, example = "1")
    private Long employeeId;
    
    @Column(name = "employee_name", length = 50)
    @ApiModelProperty(value = "员工姓名", required = true, example = "张三")
    private String employeeName;
    
    @Column(name = "employee_number", length = 20)
    @ApiModelProperty(value = "员工工号", example = "EMP001")
    private String employeeNumber;
    
    @Column(name = "department_id", nullable = false)
    @ApiModelProperty(value = "部门ID", required = true, example = "1")
    private Long departmentId;
    
    @Column(name = "department_name", length = 100)
    @ApiModelProperty(value = "部门名称", required = true, example = "技术部")
    private String departmentName;
    
    @Column(name = "department_code", length = 50)
    @ApiModelProperty(value = "部门代码", example = "TECH")
    private String departmentCode;
    
    @Column(name = "position_id")
    @ApiModelProperty(value = "岗位ID", example = "1")
    private Long positionId;
    
    @Column(name = "position_name", length = 100)
    @ApiModelProperty(value = "岗位名称", example = "高级软件工程师")
    private String positionName;
    
    @Column(name = "position_code", length = 50)
    @ApiModelProperty(value = "岗位代码", example = "SE_SENIOR")
    private String positionCode;
    
    @Column(name = "manager_id")
    @ApiModelProperty(value = "直属主管ID", example = "10")
    private Long managerId;
    
    @Column(name = "manager_name", length = 50)
    @ApiModelProperty(value = "直属主管姓名", example = "李四")
    private String managerName;
    
    @Column(name = "relation_type", length = 20)
    @ApiModelProperty(value = "关系类型", required = true, example = "NORMAL")
    private String relationType = "NORMAL";
    
    @Column(name = "effective_date")
    @ApiModelProperty(value = "生效日期", required = true)
    private LocalDateTime effectiveDate;
    
    @Column(name = "expiry_date")
    @ApiModelProperty(value = "失效日期")
    private LocalDateTime expiryDate;
    
    @Column(name = "status", length = 20)
    @ApiModelProperty(value = "状态", required = true, example = "生效")
    private String status = "ACTIVE";
    
    @Column(name = "is_primary", nullable = false)
    @ApiModelProperty(value = "是否主要关系", required = true, example = "true")
    private Boolean isPrimary = true;
    
    @Column(name = "employment_type", length = 50)
    @ApiModelProperty(value = "雇佣类型", example = "正式员工")
    private String employmentType;
    
    @Column(name = "work_location", length = 100)
    @ApiModelProperty(value = "工作地点", example = "北京总部")
    private String workLocation;
    
    @Column(name = "report_line", length = 200)
    @ApiModelProperty(value = "汇报线", example = "张三 -> 李四 -> 王五")
    private String reportLine;
    
    @Column(name = "cost_center", length = 50)
    @ApiModelProperty(value = "成本中心", example = "CC001")
    private String costCenter;
    
    @Column(name = "notes", length = 500)
    @ApiModelProperty(value = "备注", example = "兼任项目经理")
    private String notes;
    
    // 关系类型常量
    public static final String RELATION_TYPE_NORMAL = "NORMAL";          // 正常归属
    public static final String RELATION_TYPE_PART_TIME = "PART_TIME";    // 兼职
    public static final String RELATION_TYPE_TEMPORARY = "TEMPORARY";    // 临时
    public static final String RELATION_TYPE_MATRIX = "MATRIX";          // 矩阵关系
    
    // 状态常量
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_EXPIRED = "EXPIRED";
    
    // 构造函数
    public OrganizationRelation() {
        super();
        this.status = STATUS_ACTIVE;
        this.isPrimary = true;
        this.relationType = RELATION_TYPE_NORMAL;
        this.effectiveDate = LocalDateTime.now();
    }
    
    public OrganizationRelation(Long employeeId, String employeeName, Long departmentId, String departmentName) {
        this();
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
    }
    
    // JPA生命周期回调
    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.status == null) {
            this.status = STATUS_ACTIVE;
        }
        if (this.isPrimary == null) {
            this.isPrimary = true;
        }
        if (this.relationType == null) {
            this.relationType = RELATION_TYPE_NORMAL;
        }
        if (this.effectiveDate == null) {
            this.effectiveDate = LocalDateTime.now();
        }
    }
    
    /**
     * 判断关系是否有效
     */
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return STATUS_ACTIVE.equals(this.status) && 
               this.effectiveDate != null && 
               this.effectiveDate.isBefore(now.plusSeconds(1)) &&
               (this.expiryDate == null || this.expiryDate.isAfter(now));
    }
    
    /**
     * 判断是否已过期
     */
    public boolean isExpired() {
        LocalDateTime now = LocalDateTime.now();
        return this.expiryDate != null && this.expiryDate.isBefore(now);
    }
    
    /**
     * 激活关系
     */
    public void activate() {
        this.status = STATUS_ACTIVE;
        if (this.effectiveDate == null) {
            this.effectiveDate = LocalDateTime.now();
        }
    }
    
    /**
     * 终止关系
     */
    public void terminate() {
        this.status = STATUS_INACTIVE;
        this.expiryDate = LocalDateTime.now();
    }
    
    /**
     * 设置关系结束日期
     */
    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
        if (expiryDate != null && expiryDate.isBefore(LocalDateTime.now())) {
            this.status = STATUS_EXPIRED;
        }
    }
    
    @Override
    public String toString() {
        return "OrganizationRelation{" +
                "id=" + getId() +
                ", employeeId=" + employeeId +
                ", employeeName='" + employeeName + '\'' +
                ", departmentId=" + departmentId +
                ", departmentName='" + departmentName + '\'' +
                ", positionId=" + positionId +
                ", positionName='" + positionName + '\'' +
                ", managerId=" + managerId +
                ", managerName='" + managerName + '\'' +
                ", relationType='" + relationType + '\'' +
                ", status='" + status + '\'' +
                ", isPrimary=" + isPrimary +
                '}';
    }
} 