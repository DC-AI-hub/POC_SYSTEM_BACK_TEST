package demo.backed.config;

import demo.backed.entity.User;
import demo.backed.service.KeycloakUserSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Keycloak JWT认证服务
 * 负责处理JWT Token解析、用户同步和认证信息提取
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile("keycloak")
public class KeycloakJwtAuthenticationService {
    
    private final KeycloakUserSyncService keycloakUserSyncService;
    
    /**
     * 获取当前认证用户的邮箱
     */
    public Optional<String> getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtToken.getToken();
            
            String email = jwt.getClaimAsString("email");
            if (StringUtils.hasText(email)) {
                return Optional.of(email);
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * 获取当前认证用户的JWT Token
     */
    public Optional<Jwt> getCurrentJwtToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
            return Optional.of(jwtToken.getToken());
        }
        
        return Optional.empty();
    }
    
    /**
     * 获取当前用户的完整信息
     */
    public Optional<User> getCurrentUser() {
        return getCurrentJwtToken()
                .map(jwt -> {
                    try {
                        return keycloakUserSyncService.syncKeycloakUserToLocal(jwt);
                    } catch (Exception e) {
                        log.error("同步当前用户失败", e);
                        return null;
                    }
                });
    }
    
    /**
     * 从JWT中提取用户信息
     */
    public UserInfo extractUserInfo(Jwt jwt) {
        return UserInfo.builder()
                .keycloakId(jwt.getSubject())
                .email(jwt.getClaimAsString("email"))
                .username(jwt.getClaimAsString("preferred_username"))
                .name(jwt.getClaimAsString("name"))
                .employeeId(jwt.getClaimAsString("employee_id"))
                .department(jwt.getClaimAsString("department"))
                .userType(jwt.getClaimAsString("user_type"))
                .position(jwt.getClaimAsString("position"))
                .build();
    }
    
    /**
     * 验证JWT Token是否有效
     */
    public boolean isTokenValid(Jwt jwt) {
        try {
            // 检查必要的声明
            String email = jwt.getClaimAsString("email");
            String subject = jwt.getSubject();
            
            if (!StringUtils.hasText(email) || !StringUtils.hasText(subject)) {
                log.warn("JWT Token缺少必要的声明：email={}, subject={}", email, subject);
                return false;
            }
            
            // 检查Token是否过期
            java.time.Instant expiresAt = jwt.getExpiresAt();
            if (expiresAt != null && expiresAt.isBefore(java.time.Instant.now())) {
                log.warn("JWT Token已过期：{}", expiresAt);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("验证JWT Token失败", e);
            return false;
        }
    }
    
    /**
     * 用户信息封装类
     */
    @lombok.Builder
    @lombok.Data
    public static class UserInfo {
        private String keycloakId;
        private String email;
        private String username;
        private String name;
        private String employeeId;
        private String department;
        private String userType;
        private String position;
    }
} 