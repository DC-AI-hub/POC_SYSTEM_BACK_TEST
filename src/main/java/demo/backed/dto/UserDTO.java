package demo.backed.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "用户数据传输对象")
public class UserDTO {
    
    @ApiModelProperty(value = "用户ID", example = "1")
    private Long id;
    
    @ApiModelProperty(value = "工号", required = true, example = "EMP001")
    private String employeeId;
    
    @ApiModelProperty(value = "姓名", required = true, example = "张三")
    private String userName;
    
    @ApiModelProperty(value = "邮箱", required = true, example = "zhangsan@example.com")
    private String email;
    
    @ApiModelProperty(value = "电话", example = "13800138000")
    private String phone;
    
    @ApiModelProperty(value = "部门", required = true, example = "技术部")
    private String department;
    
    @ApiModelProperty(value = "岗位", example = "高级工程师")
    private String position;
    
    @ApiModelProperty(value = "用户类型", required = true, example = "员工")
    private String userType;
    
    @ApiModelProperty(value = "状态", required = true, example = "在职")
    private String status;
    
    @ApiModelProperty(value = "是否在线", example = "true")
    private Boolean isOnline;
    
    @ApiModelProperty(value = "最后登录时间")
    private LocalDateTime lastLoginTime;
    
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdTime;
    
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedTime;
    
    @ApiModelProperty(value = "入职日期")
    private LocalDateTime hireDate;
    
    @ApiModelProperty(value = "直属主管")
    private String manager;
    
    @ApiModelProperty(value = "直属主管ID")
    private Long managerId;
    
    @ApiModelProperty(value = "工作地点")
    private String workLocation;
    
    @ApiModelProperty(value = "备注")
    private String notes;
    
    @ApiModelProperty(value = "密码", example = "user123")
    private String password;
    
    @ApiModelProperty(value = "Keycloak用户ID")
    private String keycloakId;
    
    @ApiModelProperty(value = "用户角色列表")
    private java.util.List<String> roles;
} 