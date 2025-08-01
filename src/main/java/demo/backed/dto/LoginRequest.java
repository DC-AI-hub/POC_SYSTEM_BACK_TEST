package demo.backed.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
@ApiModel(description = "登录请求")
public class LoginRequest {
    
    @ApiModelProperty(value = "员工邮箱", required = true, example = "zhangsan@hkex.com")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @ApiModelProperty(value = "密码", required = true, example = "password123")
    @NotBlank(message = "密码不能为空")
    private String password;
    
    @ApiModelProperty(value = "记住登录", example = "false")
    private Boolean rememberMe = false;
    
    public LoginRequest() {}
    
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
    
    public LoginRequest(String email, String password, Boolean rememberMe) {
        this.email = email;
        this.password = password;
        this.rememberMe = rememberMe;
    }
}