package demo.backed.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@Entity
@Table(name = "t_poc_positions", indexes = {
    @Index(name = "idx_pos_code", columnList = "code", unique = true),
    @Index(name = "idx_pos_department", columnList = "department_id"),
    @Index(name = "idx_pos_level", columnList = "level"),
    @Index(name = "idx_pos_status", columnList = "status"),
    @Index(name = "idx_pos_manager", columnList = "is_manager")
})
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "岗位实体")
public class Position extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 100)
    @ApiModelProperty(value = "岗位名称", required = true, example = "高级软件工程师")
    private String name;
    
    @Column(name = "code", unique = true, length = 50)
    @ApiModelProperty(value = "岗位代码", required = true, example = "SE_SENIOR")
    private String code;
    
    @Column(name = "department_id", nullable = false)
    @ApiModelProperty(value = "所属部门ID", required = true, example = "1")
    private Long departmentId;
    
    @Column(name = "department_name", length = 100)
    @ApiModelProperty(value = "所属部门名称", required = true, example = "技术部")
    private String departmentName;
    
    @Column(name = "level", length = 20)
    @ApiModelProperty(value = "岗位级别", required = true, example = "P6")
    private String level;
    
    @Column(name = "is_manager")
    @ApiModelProperty(value = "是否管理岗位", required = true, example = "false")
    private Boolean isManager = false;
    
    @Column(name = "description", length = 500)
    @ApiModelProperty(value = "岗位描述", example = "负责核心系统的开发和维护工作")
    private String description;
    
    @Column(name = "requirements", length = 1000)
    @ApiModelProperty(value = "岗位要求", example = "计算机相关专业，3年以上开发经验")
    private String requirements;
    
    @Column(name = "employee_count")
    @ApiModelProperty(value = "在岗人数", example = "5")
    private Integer employeeCount = 0;
    
    @Column(name = "status", length = 20)
    @ApiModelProperty(value = "岗位状态", required = true, example = "正常")
    private String status = "ACTIVE";
    
    @Column(name = "sort_order")
    @ApiModelProperty(value = "排序权重", example = "1")
    private Integer sortOrder = 0;
    
    @Column(name = "max_headcount")
    @ApiModelProperty(value = "最大编制人数", example = "10")
    private Integer maxHeadcount;
    
    @Column(name = "min_salary")
    @ApiModelProperty(value = "最低薪资", example = "15000")
    private Long minSalary;
    
    @Column(name = "max_salary")
    @ApiModelProperty(value = "最高薪资", example = "25000")
    private Long maxSalary;
    
    @Column(name = "skills_required", length = 500)
    @ApiModelProperty(value = "技能要求", example = "Java,Spring Boot,MySQL,Redis")
    private String skillsRequired;
    
    @Column(name = "education_requirement", length = 50)
    @ApiModelProperty(value = "学历要求", example = "本科及以上")
    private String educationRequirement;
    
    @Column(name = "experience_requirement", length = 50)
    @ApiModelProperty(value = "经验要求", example = "3-5年")
    private String experienceRequirement;
    
    // 构造函数
    public Position() {
        super();
        this.isManager = false;
        this.employeeCount = 0;
        this.status = "ACTIVE";
        this.sortOrder = 0;
    }
    
    public Position(String name, String code, Long departmentId, String departmentName) {
        this();
        this.name = name;
        this.code = code;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
    }
    
    // JPA生命周期回调
    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.isManager == null) {
            this.isManager = false;
        }
        if (this.employeeCount == null) {
            this.employeeCount = 0;
        }
        if (this.status == null) {
            this.status = "ACTIVE";
        }
        if (this.sortOrder == null) {
            this.sortOrder = 0;
        }
    }
    
    /**
     * 判断是否为管理岗位
     */
    public boolean isManagerPosition() {
        return this.isManager != null && this.isManager;
    }
    
    /**
     * 判断是否已满编
     */
    public boolean isFullHeadcount() {
        return this.maxHeadcount != null && 
               this.employeeCount != null && 
               this.employeeCount >= this.maxHeadcount;
    }
    
    /**
     * 计算空缺人数
     */
    public int getVacancyCount() {
        if (this.maxHeadcount == null || this.employeeCount == null) {
            return 0;
        }
        return Math.max(0, this.maxHeadcount - this.employeeCount);
    }
    
    @Override
    public String toString() {
        return "Position{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", departmentId=" + departmentId +
                ", departmentName='" + departmentName + '\'' +
                ", level='" + level + '\'' +
                ", isManager=" + isManager +
                ", employeeCount=" + employeeCount +
                ", status='" + status + '\'' +
                ", sortOrder=" + sortOrder +
                '}';
    }
} 