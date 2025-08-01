package demo.backed.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import lombok.extern.slf4j.Slf4j;

/**
 * Keycloak配置类
 * 提供Keycloak Admin客户端的配置
 */
@Configuration
@Profile("keycloak")
@Slf4j
public class KeycloakConfig {
    
    @Value("${keycloak.auth-server-url}")
    private String serverUrl;
    
    @Value("${keycloak.realm}")
    private String realm;
    
    @Value("${app.keycloak.admin-client-id:admin-cli}")
    private String adminClientId;
    
    @Value("${app.keycloak.admin-username:admin}")
    private String adminUsername;
    
    @Value("${app.keycloak.admin-password:admin}")
    private String adminPassword;
    
    /**
     * 创建Keycloak Admin客户端
     * 用于管理Keycloak中的用户、角色等资源
     */
    @Bean
    public Keycloak keycloakAdmin() {
        try {
            log.info("初始化Keycloak Admin客户端，服务器URL: {}", serverUrl);
            
            Keycloak keycloak = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm("master") // 管理员登录到master realm
                    .clientId(adminClientId)
                    .username(adminUsername)
                    .password(adminPassword)
                    .build();
            
            // 测试连接
            keycloak.tokenManager().getAccessToken();
            log.info("Keycloak Admin客户端初始化成功");
            
            return keycloak;
        } catch (Exception e) {
            log.error("Keycloak Admin客户端初始化失败: {}", e.getMessage());
            throw new RuntimeException("无法连接到Keycloak服务器", e);
        }
    }
} 