package demo.backed.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_poc_users", indexes = {
    @Index(name = "idx_employee_id", columnList = "employee_id", unique = true),
    @Index(name = "idx_email", columnList = "email", unique = true),
    @Index(name = "idx_department", columnList = "department"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "用户实体")
public class User extends BaseEntity {
    

    
    @Column(name = "employee_id", nullable = false, unique = true, length = 20)
    @ApiModelProperty(value = "工号", required = true, example = "EMP001")
    private String employeeId;
    
    @Column(name = "user_name", nullable = false, length = 50)
    @ApiModelProperty(value = "姓名", required = true, example = "张三")
    private String userName;
    
    @Column(name = "email", nullable = false, unique = true, length = 100)
    @ApiModelProperty(value = "邮箱", required = true, example = "zhangsan@example.com")
    private String email;
    
    @Column(name = "phone", length = 20)
    @ApiModelProperty(value = "电话", example = "13800138000")
    private String phone;
    
    @Column(name = "department", nullable = false, length = 50)
    @ApiModelProperty(value = "部门", required = true, example = "技术部")
    private String department;
    
    @Column(name = "position", length = 50)
    @ApiModelProperty(value = "岗位", example = "高级工程师")
    private String position;
    
    @Column(name = "user_type", nullable = false, length = 20)
    @ApiModelProperty(value = "用户类型(主管/员工)", required = true, example = "员工")
    private String userType;
    
    @Column(name = "status", nullable = false, length = 20)
    @ApiModelProperty(value = "状态", required = true, example = "在职")
    private String status;
    
    @Column(name = "password", nullable = false)
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    private String password;
    
    @Column(name = "is_online")
    @ApiModelProperty(value = "是否在线", example = "true")
    private Boolean isOnline;
    
    @Column(name = "last_login_time")
    @ApiModelProperty(value = "最后登录时间")
    private LocalDateTime lastLoginTime;
    
    @Column(name = "hire_date")
    @ApiModelProperty(value = "入职日期")
    private LocalDateTime hireDate;
    
    @Column(name = "manager", length = 50)
    @ApiModelProperty(value = "直属主管")
    private String manager;
    
    @Column(name = "manager_id")
    @ApiModelProperty(value = "直属主管ID")
    private Long managerId;
    
    @Column(name = "work_location", length = 100)
    @ApiModelProperty(value = "工作地点")
    private String workLocation;
    
    @Column(name = "notes", length = 500)
    @ApiModelProperty(value = "备注")
    private String notes;
    
    @Column(name = "keycloak_id", unique = true)
    @ApiModelProperty(value = "Keycloak用户ID")
    private String keycloakId;
    
    // 构造函数
    public User() {
        super();
        this.isOnline = false;
        this.status = "在职";
        this.userType = "员工";
    }
    
    public User(String employeeId, String userName, String email, String department) {
        this();
        this.employeeId = employeeId;
        this.userName = userName;
        this.email = email;
        this.department = department;
    }
    
    // JPA生命周期回调
    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.isOnline == null) {
            this.isOnline = false;
        }
        if (this.status == null) {
            this.status = "在职";
        }
        if (this.userType == null) {
            this.userType = "员工";
        }
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + getId() +
                ", employeeId='" + employeeId + '\'' +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", department='" + department + '\'' +
                ", position='" + position + '\'' +
                ", userType='" + userType + '\'' +
                ", status='" + status + '\'' +
                ", isOnline=" + isOnline +
                ", lastLoginTime=" + lastLoginTime +
                '}';
    }
} 