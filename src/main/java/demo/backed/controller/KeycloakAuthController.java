package demo.backed.controller;

import demo.backed.config.KeycloakJwtAuthenticationService;
import demo.backed.dto.ApiResponse;
import demo.backed.entity.User;
import demo.backed.service.KeycloakUserSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Keycloak认证控制器
 * 提供Keycloak相关的认证和用户同步功能
 */
@RestController
@RequestMapping("/api/auth/keycloak")
@RequiredArgsConstructor
@Slf4j
@Profile("keycloak")
public class KeycloakAuthController {
    
    private final KeycloakUserSyncService keycloakUserSyncService;
    private final KeycloakJwtAuthenticationService keycloakJwtAuthenticationService;
    
    /**
     * 获取当前认证用户信息
     */
    @GetMapping("/user-info")
    @PreAuthorize("hasRole('hkex-user')")
    public ResponseEntity<ApiResponse<User>> getCurrentUserInfo() {
        try {
            Optional<User> currentUser = keycloakJwtAuthenticationService.getCurrentUser();
            
            if (currentUser.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success("获取用户信息成功", currentUser.get()));
            } else {
                return ResponseEntity.ok(ApiResponse.error("未找到当前用户信息"));
            }
        } catch (Exception e) {
            log.error("获取当前用户信息失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取用户信息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取当前用户的JWT Token信息
     */
    @GetMapping("/token-info")
    @PreAuthorize("hasRole('hkex-user')")
    public ResponseEntity<ApiResponse<KeycloakJwtAuthenticationService.UserInfo>> getCurrentTokenInfo() {
        try {
            Optional<Jwt> jwt = keycloakJwtAuthenticationService.getCurrentJwtToken();
            
            if (jwt.isPresent()) {
                KeycloakJwtAuthenticationService.UserInfo userInfo = keycloakJwtAuthenticationService.extractUserInfo(jwt.get());
                return ResponseEntity.ok(ApiResponse.success("获取Token信息成功", userInfo));
            } else {
                return ResponseEntity.ok(ApiResponse.error("未找到JWT Token"));
            }
        } catch (Exception e) {
            log.error("获取Token信息失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取Token信息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 手动同步用户到Keycloak
     */
    @PostMapping("/sync-user")
    @PreAuthorize("hasRole('hkex-manager')")
    public ResponseEntity<ApiResponse<Boolean>> syncUserToKeycloak(@RequestParam String email) {
        try {
            boolean success = keycloakUserSyncService.syncUserToKeycloak(email);
            
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("用户同步成功", true));
            } else {
                return ResponseEntity.ok(ApiResponse.success("用户同步失败", false));
            }
        } catch (Exception e) {
            log.error("同步用户到Keycloak失败: {}", email, e);
            return ResponseEntity.ok(ApiResponse.error("同步用户失败: " + e.getMessage()));
        }
    }
    
    /**
     * 触发所有用户同步
     */
    @PostMapping("/sync-all")
    @PreAuthorize("hasRole('hkex-manager')")
    public ResponseEntity<ApiResponse<String>> syncAllUsers() {
        try {
            keycloakUserSyncService.syncExistingUsersToKeycloak();
            return ResponseEntity.ok(ApiResponse.success("批量同步已启动", "请查看日志了解详细结果"));
        } catch (Exception e) {
            log.error("批量同步用户失败", e);
            return ResponseEntity.ok(ApiResponse.error("批量同步失败: " + e.getMessage()));
        }
    }
    
    /**
     * 验证当前Token是否有效
     */
    @GetMapping("/validate-token")
    @PreAuthorize("hasRole('hkex-user')")
    public ResponseEntity<ApiResponse<Boolean>> validateCurrentToken() {
        try {
            Optional<Jwt> jwt = keycloakJwtAuthenticationService.getCurrentJwtToken();
            
            if (jwt.isPresent()) {
                boolean isValid = keycloakJwtAuthenticationService.isTokenValid(jwt.get());
                return ResponseEntity.ok(ApiResponse.success("Token验证完成", isValid));
            } else {
                return ResponseEntity.ok(ApiResponse.error("未找到JWT Token"));
            }
        } catch (Exception e) {
            log.error("验证Token失败", e);
            return ResponseEntity.ok(ApiResponse.error("验证Token失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取当前用户邮箱
     */
    @GetMapping("/current-email")
    @PreAuthorize("hasRole('hkex-user')")
    public ResponseEntity<ApiResponse<String>> getCurrentUserEmail() {
        try {
            Optional<String> email = keycloakJwtAuthenticationService.getCurrentUserEmail();
            
            if (email.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success("获取邮箱成功", email.get()));
            } else {
                return ResponseEntity.ok(ApiResponse.error("未找到用户邮箱"));
            }
        } catch (Exception e) {
            log.error("获取当前用户邮箱失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取邮箱失败: " + e.getMessage()));
        }
    }
    
    /**
     * 刷新本地用户信息（从Keycloak同步）
     * 修改权限：允许任何认证用户同步自己的基本信息
     */
    @PostMapping("/refresh-user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<User>> refreshCurrentUser() {
        try {
            Optional<Jwt> jwt = keycloakJwtAuthenticationService.getCurrentJwtToken();
            
            if (jwt.isPresent()) {
                User user = keycloakUserSyncService.syncKeycloakUserToLocal(jwt.get());
                return ResponseEntity.ok(ApiResponse.success("用户信息刷新成功", user));
            } else {
                return ResponseEntity.ok(ApiResponse.error("未找到JWT Token"));
            }
        } catch (Exception e) {
            log.error("刷新用户信息失败", e);
            return ResponseEntity.ok(ApiResponse.error("刷新用户信息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Keycloak认证服务正常", "OK"));
    }
} 