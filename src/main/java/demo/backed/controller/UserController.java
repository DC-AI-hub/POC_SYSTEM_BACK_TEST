package demo.backed.controller;

import demo.backed.config.KeycloakJwtAuthenticationService;
import demo.backed.dto.ApiResponse;
import demo.backed.dto.LoginRequest;
import demo.backed.dto.LoginResponse;
import demo.backed.dto.UserDTO;
import demo.backed.service.KeycloakUserSyncService;
import demo.backed.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@Api(tags = "用户管理", description = "用户管理相关API")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired(required = false)
    private KeycloakUserSyncService keycloakUserSyncService;
    
    @Autowired(required = false)
    private KeycloakJwtAuthenticationService keycloakJwtAuthenticationService;
    
    /**
     * 获取用户列表（分页和搜索）
     */
    @GetMapping
    @ApiOperation("获取用户列表")
    @PreAuthorize("hasRole('hkex-user')")
    public ApiResponse<Page<UserDTO>> getUsers(
            @ApiParam("页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @ApiParam("每页大小") @RequestParam(defaultValue = "100") int size,
            @ApiParam("搜索关键词") @RequestParam(required = false) String keyword,
            @ApiParam("部门") @RequestParam(required = false) String department,
            @ApiParam("状态") @RequestParam(required = false) String status,
            @ApiParam("用户类型") @RequestParam(required = false) String userType) {
        
        try {
            Page<UserDTO> users = userService.getUsers(page, size, keyword, department, status, userType);
            return ApiResponse.success(users);
        } catch (Exception e) {
            return ApiResponse.error("获取用户列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据ID获取用户详情
     */
    @GetMapping("/{id}")
    @ApiOperation("获取用户详情")
    @PreAuthorize("hasRole('hkex-user')")
    public ApiResponse<UserDTO> getUserById(@ApiParam("用户ID") @PathVariable Long id) {
        try {
            Optional<UserDTO> user = userService.getUserById(id);
            if (user.isPresent()) {
                return ApiResponse.success(user.get());
            } else {
                return ApiResponse.notFound("用户不存在");
            }
        } catch (Exception e) {
            return ApiResponse.error("获取用户详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据邮箱获取用户详情 - 新增：支持邮箱作为唯一键
     */
    @GetMapping("/email/{email}")
    @ApiOperation("根据邮箱获取用户详情")
    @PreAuthorize("hasRole('hkex-user')")
    public ApiResponse<UserDTO> getUserByEmail(@ApiParam("邮箱") @PathVariable String email) {
        try {
            Optional<UserDTO> user = userService.getUserByEmail(email);
            if (user.isPresent()) {
                return ApiResponse.success(user.get());
            } else {
                return ApiResponse.notFound("用户不存在");
            }
        } catch (Exception e) {
            return ApiResponse.error("获取用户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建用户
     */
    @PostMapping
    @ApiOperation("创建用户")
    @PreAuthorize("hasRole('hkex-manager')")
    public ApiResponse<UserDTO> createUser(@RequestBody UserDTO userDTO) {
        try {
            UserDTO createdUser = userService.createUser(userDTO);
            
            // 如果启用了Keycloak，同步用户到Keycloak
            if (keycloakUserSyncService != null && createdUser.getEmail() != null) {
                keycloakUserSyncService.syncUserToKeycloak(createdUser.getEmail());
            }
            
            return ApiResponse.success("用户创建成功", createdUser);
        } catch (Exception e) {
            return ApiResponse.badRequest("创建用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    @ApiOperation("更新用户信息")
    @PreAuthorize("hasRole('hkex-manager')")
    public ApiResponse<UserDTO> updateUser(
            @ApiParam("用户ID") @PathVariable Long id,
            @RequestBody UserDTO userDTO) {
        try {
            UserDTO updatedUser = userService.updateUser(id, userDTO);
            
            // 如果启用了Keycloak，同步更新到Keycloak
            if (keycloakUserSyncService != null && updatedUser.getEmail() != null) {
                keycloakUserSyncService.syncUserToKeycloak(updatedUser.getEmail());
            }
            
            return ApiResponse.success("用户更新成功", updatedUser);
        } catch (Exception e) {
            return ApiResponse.badRequest("更新用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除用户")
    @PreAuthorize("hasRole('hkex-manager')")
    public ApiResponse<Void> deleteUser(@ApiParam("用户ID") @PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ApiResponse.success("用户删除成功", null);
        } catch (Exception e) {
            return ApiResponse.badRequest("删除用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取在线用户列表
     */
    @GetMapping("/online")
    @ApiOperation("获取在线用户列表")
    @PreAuthorize("hasRole('hkex-user')")
    public ApiResponse<List<UserDTO>> getOnlineUsers() {
        try {
            List<UserDTO> onlineUsers = userService.getOnlineUsers();
            return ApiResponse.success(onlineUsers);
        } catch (Exception e) {
            return ApiResponse.error("获取在线用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 用户登录 - 仅在非Keycloak模式下使用
     */
    @PostMapping("/login")
    @ApiOperation("用户登录")
    @Profile("!keycloak")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse loginResponse = userService.login(loginRequest);
            return ApiResponse.success("登录成功", loginResponse);
        } catch (Exception e) {
            return ApiResponse.unauthorized("登录失败: " + e.getMessage());
        }
    }
    
    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @ApiOperation("用户登出")
    @PreAuthorize("hasRole('hkex-user')")
    public ApiResponse<Void> logout(@RequestParam Long userId) {
        try {
            userService.logout(userId);
            return ApiResponse.success("登出成功", null);
        } catch (Exception e) {
            return ApiResponse.error("登出失败: " + e.getMessage());
        }
    }
    
    /**
     * 重置用户密码 - 仅在非Keycloak模式下使用
     */
    @PostMapping("/{id}/reset-password")
    @ApiOperation("重置用户密码")
    @PreAuthorize("hasRole('hkex-manager')")
    @Profile("!keycloak")
    public ApiResponse<Void> resetPassword(
            @ApiParam("用户ID") @PathVariable Long id,
            @RequestBody Map<String, String> passwordData) {
        try {
            String newPassword = passwordData.get("newPassword");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ApiResponse.badRequest("新密码不能为空");
            }
            userService.resetPassword(id, newPassword);
            return ApiResponse.success("密码重置成功", null);
        } catch (Exception e) {
            return ApiResponse.badRequest("密码重置失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户统计信息
     */
    @GetMapping("/stats")
    @ApiOperation("获取用户统计信息")
    @PreAuthorize("hasRole('hkex-manager')")
    public ApiResponse<Map<String, Object>> getUserStats() {
        try {
            long onlineCount = userService.getOnlineUserCount();
            List<Object[]> departmentStats = userService.getUserStatsByDepartment();
            
            Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("onlineCount", onlineCount);
            stats.put("departmentStats", departmentStats);
            
            return ApiResponse.success(stats);
        } catch (Exception e) {
            return ApiResponse.error("获取用户统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据工号获取用户
     */
    @GetMapping("/employee/{employeeId}")
    @ApiOperation("根据工号获取用户")
    @PreAuthorize("hasRole('hkex-user')")
    public ApiResponse<UserDTO> getUserByEmployeeId(@ApiParam("工号") @PathVariable String employeeId) {
        try {
            Optional<UserDTO> user = userService.getUserByEmployeeId(employeeId);
            if (user.isPresent()) {
                return ApiResponse.success(user.get());
            } else {
                return ApiResponse.notFound("用户不存在");
            }
        } catch (Exception e) {
            return ApiResponse.error("获取用户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前登录用户信息 - 新增：支持Keycloak和传统认证
     */
    @GetMapping("/current")
    @ApiOperation("获取当前登录用户信息")
    @PreAuthorize("hasRole('hkex-user')")
    public ApiResponse<UserDTO> getCurrentUser() {
        try {
            // 如果是Keycloak模式，使用JWT获取用户信息
            if (keycloakJwtAuthenticationService != null) {
                Optional<String> email = keycloakJwtAuthenticationService.getCurrentUserEmail();
                if (email.isPresent()) {
                    Optional<UserDTO> user = userService.getUserByEmail(email.get());
                    if (user.isPresent()) {
                        return ApiResponse.success(user.get());
                    }
                }
            }
            
            // 传统模式或获取失败时
            return ApiResponse.error("未找到当前用户信息");
        } catch (Exception e) {
            return ApiResponse.error("获取当前用户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 同步用户到Keycloak - 新增：Keycloak集成功能
     */
    @PostMapping("/{id}/sync-to-keycloak")
    @ApiOperation("同步用户到Keycloak")
    @PreAuthorize("hasRole('hkex-manager')")
    @Profile("keycloak")
    public ApiResponse<Boolean> syncUserToKeycloak(@ApiParam("用户ID") @PathVariable Long id) {
        try {
            if (keycloakUserSyncService == null) {
                return ApiResponse.error("Keycloak同步服务未启用");
            }
            
            Optional<UserDTO> user = userService.getUserById(id);
            if (!user.isPresent()) {
                return ApiResponse.notFound("用户不存在");
            }
            
            if (user.get().getEmail() == null) {
                return ApiResponse.badRequest("用户邮箱为空，无法同步到Keycloak");
            }
            
            boolean success = keycloakUserSyncService.syncUserToKeycloak(user.get().getEmail());
            if (success) {
                return ApiResponse.success("用户同步到Keycloak成功", true);
            } else {
                return ApiResponse.error("用户同步到Keycloak失败");
            }
        } catch (Exception e) {
            return ApiResponse.error("同步用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证邮箱唯一性 - 新增：支持邮箱作为唯一键
     */
    @GetMapping("/validate-email")
    @ApiOperation("验证邮箱唯一性")
    @PreAuthorize("hasRole('hkex-manager')")
    public ApiResponse<Boolean> validateEmailUnique(
            @ApiParam("邮箱") @RequestParam String email,
            @ApiParam("排除的用户ID") @RequestParam(required = false) Long excludeUserId) {
        try {
            boolean isUnique = userService.isEmailUnique(email, excludeUserId);
            return ApiResponse.success("邮箱唯一性验证完成", isUnique);
        } catch (Exception e) {
            return ApiResponse.error("验证邮箱唯一性失败: " + e.getMessage());
        }
    }
    
    /**
     * 导入用户数据（CSV文件）
     */
    @PostMapping("/import")
    @ApiOperation("导入用户数据")
    @PreAuthorize("hasRole('hkex-manager')")
    public ApiResponse<Map<String, Object>> importUsers(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ApiResponse.badRequest("请选择要导入的文件");
            }
            
            String fileName = file.getOriginalFilename();
            if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
                return ApiResponse.badRequest("请上传CSV格式的文件");
            }
            
            Map<String, Object> result = userService.importUsersFromCsv(file);
            return ApiResponse.success("导入完成", result);
        } catch (Exception e) {
            return ApiResponse.error("导入失败: " + e.getMessage());
        }
    }
} 