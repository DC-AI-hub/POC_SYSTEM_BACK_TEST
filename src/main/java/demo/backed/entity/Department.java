package demo.backed.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.persistence.*;
import java.util.List;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "t_poc_departments", indexes = {
    @Index(name = "idx_dept_code", columnList = "code", unique = true),
    @Index(name = "idx_dept_parent", columnList = "parent_id"),
    @Index(name = "idx_dept_manager", columnList = "manager_id"),
    @Index(name = "idx_dept_level", columnList = "level"),
    @Index(name = "idx_dept_status", columnList = "status")
})
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "部门实体")
public class Department extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 100)
    @ApiModelProperty(value = "部门名称", required = true, example = "技术部")
    private String name;
    
    @Column(name = "code", unique = true, length = 50)
    @ApiModelProperty(value = "部门代码", required = true, example = "TECH")
    private String code;
    
    @Column(name = "parent_id")
    @ApiModelProperty(value = "父部门ID", example = "1")
    private Long parentId;
    
    @Column(name = "level")
    @ApiModelProperty(value = "部门层级", required = true, example = "1")
    private Integer level;
    
    @Column(name = "description", length = 500)
    @ApiModelProperty(value = "部门描述", example = "负责技术研发工作")
    private String description;
    
    @Column(name = "manager_id")
    @ApiModelProperty(value = "部门负责人ID", example = "10")
    private Long managerId;
    
    @Column(name = "manager_name", length = 50)
    @ApiModelProperty(value = "部门负责人姓名", example = "张三")
    private String managerName;
    
    @Column(name = "employee_count")
    @ApiModelProperty(value = "部门人数", example = "15")
    private Integer employeeCount = 0;
    
    @Column(name = "status", length = 20)
    @ApiModelProperty(value = "部门状态", required = true, example = "正常")
    private String status = "ACTIVE";
    
    @Column(name = "sort_order")
    @ApiModelProperty(value = "排序权重", example = "1")
    private Integer sortOrder = 0;
    
    @Column(name = "contact_phone", length = 50)
    @ApiModelProperty(value = "联系电话", example = "010-12345678")
    private String contactPhone;
    
    @Column(name = "location", length = 200)
    @ApiModelProperty(value = "办公地点", example = "北京总部A座15层")
    private String location;
    
    @Column(name = "cost_center", length = 50)
    @ApiModelProperty(value = "成本中心", example = "CC001")
    private String costCenter;
    
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private List<Department> children;
    
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private List<Position> positions;
    
    @Transient
    @ApiModelProperty(value = "父部门名称", example = "技术部")
    private String parentName;
    
    @Transient
    @ApiModelProperty(value = "部门路径", example = "/1/2/3/")
    private String path;
    
    // 构造函数
    public Department() {
        super();
        this.level = 1;
        this.employeeCount = 0;
        this.status = "ACTIVE";
        this.sortOrder = 0;
    }
    
    public Department(String name, String code) {
        this();
        this.name = name;
        this.code = code;
    }
    
    // JPA生命周期回调
    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.level == null) {
            this.level = 1;
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
        // 生成部门路径
        generatePath();
    }
    
    @PreUpdate
    @Override
    protected void onUpdate() {
        super.onUpdate();
        // 更新部门路径
        generatePath();
    }
    
    /**
     * 生成部门路径
     */
    private void generatePath() {
        if (this.parentId == null) {
            this.path = "/" + this.getId() + "/";
        } else {
            // 这里需要在Service层处理，因为需要查询父部门
            // 暂时设置基础路径，实际路径在Service层更新
            this.path = "/" + this.getId() + "/";
        }
    }
    
    /**
     * 判断是否为根部门
     */
    public boolean isRoot() {
        return this.parentId == null;
    }
    
    /**
     * 判断是否为叶子部门（无下级部门）
     */
    public boolean isLeaf() {
        // 这个方法需要在Service层实现，需要查询数据库
        return false;
    }
    
    @Override
    public String toString() {
        return "Department{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", parentId=" + parentId +
                ", level=" + level +
                ", managerId=" + managerId +
                ", managerName='" + managerName + '\'' +
                ", employeeCount=" + employeeCount +
                ", status='" + status + '\'' +
                ", sortOrder=" + sortOrder +
                '}';
    }
} 