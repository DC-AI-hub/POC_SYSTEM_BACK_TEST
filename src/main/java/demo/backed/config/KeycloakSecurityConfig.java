package demo.backed.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Keycloak SSO安全配置
 * 使用Spring Security OAuth2 Resource Server集成Keycloak
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Profile("keycloak") // 使用Profile控制是否启用Keycloak
@Slf4j
public class KeycloakSecurityConfig {
    
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;
    
    /**
     * 配置Security过滤器链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("配置Keycloak安全过滤器链");
        
        http
            // 禁用CSRF（使用JWT不需要CSRF保护）
            .csrf().disable()
            
            // 配置CORS
            .cors().configurationSource(corsConfigurationSource())
            
            .and()
            
            // 配置会话管理为无状态
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            
            .and()
            
            // 配置URL权限
            .authorizeRequests()
                // 公开访问的接口
                .antMatchers("/api/health", "/api/auth/keycloak/health").permitAll()
                .antMatchers("/api/test/**").permitAll()
                
                // Keycloak认证相关接口 - 只要认证即可访问
                .antMatchers("/api/auth/keycloak/refresh-user").authenticated()
                .antMatchers("/api/auth/keycloak/user-info").authenticated()
                .antMatchers("/api/auth/keycloak/current-email").authenticated()
                .antMatchers("/api/auth/keycloak/validate-token").authenticated()
                .antMatchers("/api/auth/keycloak/token-info").authenticated()
                .antMatchers("/api/auth/keycloak/sync-user").hasRole("hkex-manager")
                .antMatchers("/api/auth/keycloak/sync-all").hasRole("hkex-manager")
                
                // Swagger相关路径
                .antMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                .antMatchers("/swagger-resources/**").permitAll()
                .antMatchers("/v2/api-docs", "/v3/api-docs/**").permitAll()
                .antMatchers("/webjars/**").permitAll()
                
                // 健康检查
                .antMatchers("/actuator/health").permitAll()
                
                // 错误页面
                .antMatchers("/error").permitAll()
                
                // 静态资源
                .antMatchers(HttpMethod.GET, "/", "/favicon.ico", "/**/*.html", "/**/*.css", "/**/*.js").permitAll()
                
                // 需要认证的端点 - 使用Keycloak角色
                .antMatchers(HttpMethod.GET, "/api/users/**").hasRole("hkex-user")
                .antMatchers(HttpMethod.POST, "/api/users").hasRole("hkex-manager")
                .antMatchers(HttpMethod.PUT, "/api/users/**").hasRole("hkex-manager")
                .antMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("hkex-manager")
                
                // 费用申请接口
                .antMatchers("/api/expense/**").hasRole("hkex-user")
                
                // 工作流接口
                .antMatchers("/api/workflow/**").hasRole("hkex-user")
                .antMatchers("/api/workflow-tracker/**").hasRole("hkex-user")
                
                // 审批管理接口
                .antMatchers("/api/approval/**").hasRole("hkex-user")
                
                // 系统管理接口
                .antMatchers("/api/admin/**").hasRole("hkex-manager")
                
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            
            .and()
            
            // 配置OAuth2资源服务器
            .oauth2ResourceServer()
                .jwt()
                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                .decoder(jwtDecoder());
        
        return http.build();
    }
    
    /**
     * JWT解码器
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        log.info("配置JWT解码器，发行者URI: {}", issuerUri);
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }
    
    /**
     * 配置JWT认证转换器 - 重点：支持邮箱作为唯一键
     * 从Keycloak的JWT中提取角色和用户信息
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        
        // 配置权限转换器
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            log.debug("处理JWT权限转换，用户邮箱: {}", jwt.getClaimAsString("email"));
            
            // 提取realm角色
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            Collection<String> realmRoles;
            if (realmAccess != null && realmAccess.get("roles") instanceof Collection) {
                @SuppressWarnings("unchecked")
                Collection<String> roles = (Collection<String>) realmAccess.get("roles");
                realmRoles = roles;
            } else {
                realmRoles = Collections.emptyList();
            }
            
            // 提取client角色（如果需要）
            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            Collection<String> clientRoles = new ArrayList<>();
            if (resourceAccess != null) {
                String clientId = jwt.getClaimAsString("azp");
                if (clientId != null) {
                    Object clientAccessObj = resourceAccess.get(clientId);
                    if (clientAccessObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> clientAccess = (Map<String, Object>) clientAccessObj;
                        if (clientAccess.containsKey("roles")) {
                            Object rolesObj = clientAccess.get("roles");
                            if (rolesObj instanceof Collection) {
                                @SuppressWarnings("unchecked")
                                Collection<String> roles = (Collection<String>) rolesObj;
                                clientRoles = roles;
                            }
                        }
                    }
                }
            }
            
            // 合并所有角色
            Set<GrantedAuthority> authorities = new HashSet<>();
            realmRoles.forEach(role -> {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                log.debug("添加realm角色: ROLE_{}", role);
            });
            clientRoles.forEach(role -> {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                log.debug("添加client角色: ROLE_{}", role);
            });
            
            // 检查是否是管理员用户（根据邮箱或用户类型）
            String email = jwt.getClaimAsString("email");
            String userType = jwt.getClaimAsString("user_type");
            boolean isAdmin = false;
            
            if (email != null && email.equals("admin@hkex.com")) {
                isAdmin = true;
            }
            if ("主管".equals(userType) || "管理员".equals(userType)) {
                isAdmin = true;
            }
            // 检查是否有管理员相关的角色
            if (clientRoles.contains("admin") || clientRoles.contains("manager") || 
                realmRoles.contains("admin") || realmRoles.contains("manager")) {
                isAdmin = true;
            }
            
            // 为所有认证用户添加基础角色
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            authorities.add(new SimpleGrantedAuthority("ROLE_hkex-user"));
            log.debug("添加基础用户角色: ROLE_USER, ROLE_hkex-user");
            
            // 为管理员用户添加管理员角色
            if (isAdmin) {
                authorities.add(new SimpleGrantedAuthority("ROLE_MANAGER"));
                authorities.add(new SimpleGrantedAuthority("ROLE_hkex-manager"));
                log.debug("添加管理员角色: ROLE_MANAGER, ROLE_hkex-manager");
            }
            
            log.debug("最终用户角色列表: {}", authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
            
            return authorities;
        });
        
        // 重点：设置Principal名称为邮箱地址（支持邮箱作为唯一键）
        converter.setPrincipalClaimName("email");
        
        return converter;
    }
    
    /**
     * 配置CORS跨域
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 允许的源
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 允许发送凭据
        configuration.setAllowCredentials(true);
        
        // 预检请求的缓存时间（秒）
        configuration.setMaxAge(3600L);
        
        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Total-Count"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
} 