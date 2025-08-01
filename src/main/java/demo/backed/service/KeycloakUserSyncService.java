package demo.backed.service;

import demo.backed.entity.User;
import demo.backed.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Keycloak用户同步服务
 * 负责本地用户和Keycloak用户的双向同步
 * 重点：以邮箱作为唯一标识符
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Profile("keycloak")
public class KeycloakUserSyncService {
    
    private final UserRepository userRepository;
    private final Keycloak keycloakAdmin;
    
    @Value("${keycloak.realm}")
    private String realmName;
    
    @Value("${app.keycloak.sync-users:true}")
    private boolean syncEnabled;
    
    @Value("${app.keycloak.default-password:Hkex@2024}")
    private String defaultPassword;
    
    /**
     * 应用启动后同步现有用户到Keycloak
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void syncExistingUsersToKeycloak() {
        if (!syncEnabled) {
            log.info("用户同步已禁用");
            return;
        }
        
        log.info("开始同步本地用户到Keycloak...");
        
        try {
            RealmResource realm = keycloakAdmin.realm(realmName);
            UsersResource usersResource = realm.users();
            
            // 获取所有在职用户
            List<User> activeUsers = userRepository.findByStatus("在职");
            log.info("找到 {} 个需要同步的用户", activeUsers.size());
            
            int successCount = 0;
            int skipCount = 0;
            int failCount = 0;
            
            for (User user : activeUsers) {
                try {
                    // 验证用户邮箱
                    if (!StringUtils.hasText(user.getEmail())) {
                        log.warn("用户 {} 没有邮箱地址，跳过同步", user.getUserName());
                        failCount++;
                        continue;
                    }
                    
                    // 重点：以邮箱作为唯一标识符检查用户是否已存在
                    Optional<UserRepresentation> existingUser = findKeycloakUserByEmail(usersResource, user.getEmail());
                    
                    if (existingUser.isPresent()) {
                        // 用户已存在，更新本地的Keycloak ID
                        String keycloakId = existingUser.get().getId();
                        if (!keycloakId.equals(user.getKeycloakId())) {
                            user.setKeycloakId(keycloakId);
                            userRepository.save(user);
                            log.info("更新用户 {} 的Keycloak ID: {}", user.getEmail(), keycloakId);
                        }
                        
                        // 同步用户信息到Keycloak
                        updateKeycloakUser(realm, keycloakId, user);
                        skipCount++;
                        continue;
                    }
                    
                    // 创建新的Keycloak用户
                    UserRepresentation keycloakUser = createKeycloakUser(user);
                    Response response = usersResource.create(keycloakUser);
                    
                    if (response.getStatus() == 201) {
                        // 获取创建的用户ID
                        String userId = getCreatedId(response);
                        response.close();
                        
                        // 设置密码
                        setUserPassword(realm, userId);
                        
                        // 分配角色
                        assignUserRoles(realm, userId, user.getUserType());
                        
                        // 更新本地用户的Keycloak ID
                        user.setKeycloakId(userId);
                        userRepository.save(user);
                        
                        log.info("成功同步用户: {} ({})", user.getUserName(), user.getEmail());
                        successCount++;
                    } else {
                        log.error("创建Keycloak用户失败: {} - 状态码: {}", user.getEmail(), response.getStatus());
                        response.close();
                        failCount++;
                    }
                    
                } catch (WebApplicationException e) {
                    log.error("同步用户 {} 时发生WebApplicationException: {}", user.getEmail(), e.getMessage());
                    failCount++;
                } catch (Exception e) {
                    log.error("同步用户 {} 时发生错误: {}", user.getEmail(), e.getMessage());
                    failCount++;
                }
            }
            
            log.info("用户同步完成: 成功={}, 跳过={}, 失败={}", successCount, skipCount, failCount);
            
        } catch (Exception e) {
            log.error("用户同步过程中发生错误", e);
        }
    }
    
    /**
     * 当Keycloak用户登录时同步或更新本地用户
     * 重点：以邮箱作为唯一标识符
     */
    @Transactional
    public User syncKeycloakUserToLocal(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String username = jwt.getClaimAsString("preferred_username");
        String name = jwt.getClaimAsString("name");
        
        log.info("开始同步Keycloak用户到本地: 邮箱={}, Keycloak ID={}", email, keycloakId);
        
        // 验证必要的字段
        if (!StringUtils.hasText(email)) {
            log.error("Keycloak用户没有邮箱地址，无法同步");
            throw new IllegalArgumentException("用户邮箱不能为空");
        }
        
        // 重试机制处理并发冲突
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                return doSyncKeycloakUserToLocal(jwt, keycloakId, email, username, name);
            } catch (org.springframework.orm.ObjectOptimisticLockingFailureException | 
                     org.hibernate.StaleStateException e) {
                log.warn("用户同步发生版本冲突，重试 {}/{}: {}", i + 1, maxRetries, e.getMessage());
                if (i == maxRetries - 1) {
                    log.error("用户同步重试失败，返回现有用户信息");
                    // 最后一次重试失败，直接返回现有用户
                    return userRepository.findByEmail(email)
                            .orElseThrow(() -> new RuntimeException("用户同步失败且无法找到现有用户"));
                }
                // 等待一小段时间后重试
                try {
                    Thread.sleep(50 * (i + 1)); // 递增等待时间
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("用户同步被中断");
                }
            }
        }
        throw new RuntimeException("用户同步失败");
    }
    
    private User doSyncKeycloakUserToLocal(Jwt jwt, String keycloakId, String email, String username, String name) {
        // 自定义属性
        String employeeId = jwt.getClaimAsString("employee_id");
        String department = jwt.getClaimAsString("department");
        String userType = jwt.getClaimAsString("user_type");
        String position = jwt.getClaimAsString("position");
        
        // 重点：优先通过邮箱查找用户，其次通过Keycloak ID
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.findByKeycloakId(keycloakId)
                        .orElseGet(() -> {
                            log.info("创建新用户: {}", email);
                            User newUser = new User();
                            newUser.setEmail(email);
                            newUser.setKeycloakId(keycloakId);
                            newUser.setCreatedTime(LocalDateTime.now());
                            newUser.setStatus("在职");
                            return newUser;
                        }));
        
        // 更新用户信息
        boolean updated = false;
        
        if (StringUtils.hasText(employeeId) && !employeeId.equals(user.getEmployeeId())) {
            user.setEmployeeId(employeeId);
            updated = true;
        }
        
        if (StringUtils.hasText(name) && !name.equals(user.getUserName())) {
            user.setUserName(name);
            updated = true;
        } else if (StringUtils.hasText(username) && !username.equals(user.getUserName())) {
            user.setUserName(username);
            updated = true;
        }
        
        if (StringUtils.hasText(department) && !department.equals(user.getDepartment())) {
            user.setDepartment(department);
            updated = true;
        }
        
        if (StringUtils.hasText(position) && !position.equals(user.getPosition())) {
            user.setPosition(position);
            updated = true;
        }
        
        String determinedUserType = StringUtils.hasText(userType) ? userType : determineUserType(jwt);
        if (!determinedUserType.equals(user.getUserType())) {
            user.setUserType(determinedUserType);
            updated = true;
        }
        
        // 确保Keycloak ID正确
        if (!keycloakId.equals(user.getKeycloakId())) {
            user.setKeycloakId(keycloakId);
            updated = true;
        }
        
        // 只有在有实际更新时才更新时间戳，减少不必要的版本冲突
        LocalDateTime now = LocalDateTime.now();
        if (updated || user.getLastLoginTime() == null || 
            user.getLastLoginTime().isBefore(now.minusMinutes(5))) { // 5分钟内不重复更新登录时间
            user.setLastLoginTime(now);
            user.setUpdatedTime(now);
            updated = true;
        }
        
        User savedUser = userRepository.save(user);
        
        if (updated) {
            log.info("用户信息已更新: {}", email);
        } else {
            log.debug("用户信息无变化: {}", email);
        }
        
        return savedUser;
    }
    
    /**
     * 重点：通过邮箱查找Keycloak用户
     */
    private Optional<UserRepresentation> findKeycloakUserByEmail(UsersResource usersResource, String email) {
        try {
            List<UserRepresentation> users = usersResource.search(email, true);
            return users.stream()
                    .filter(user -> email.equals(user.getEmail()))
                    .findFirst();
        } catch (Exception e) {
            log.error("查找Keycloak用户失败，邮箱: {}", email, e);
            return Optional.empty();
        }
    }
    
    /**
     * 更新Keycloak用户信息
     */
    private void updateKeycloakUser(RealmResource realm, String keycloakId, User user) {
        try {
            UserResource userResource = realm.users().get(keycloakId);
            UserRepresentation userRep = userResource.toRepresentation();
            
            // 更新基本信息
            userRep.setFirstName(user.getUserName());
            userRep.setEnabled("在职".equals(user.getStatus()));
            
            // 更新自定义属性
            Map<String, List<String>> attributes = userRep.getAttributes();
            if (attributes == null) {
                attributes = new HashMap<>();
            }
            
            attributes.put("employee_id", Collections.singletonList(user.getEmployeeId()));
            attributes.put("department", Collections.singletonList(user.getDepartment()));
            attributes.put("user_type", Collections.singletonList(user.getUserType()));
            attributes.put("position", Collections.singletonList(user.getPosition()));
            
            userRep.setAttributes(attributes);
            
            userResource.update(userRep);
            log.debug("更新Keycloak用户信息: {}", user.getEmail());
        } catch (Exception e) {
            log.error("更新Keycloak用户失败: {}", user.getEmail(), e);
        }
    }
    
    /**
     * 创建Keycloak用户表示
     */
    private UserRepresentation createKeycloakUser(User user) {
        UserRepresentation keycloakUser = new UserRepresentation();
        keycloakUser.setUsername(user.getEmail()); // 重点：使用邮箱作为用户名
        keycloakUser.setEmail(user.getEmail());
        keycloakUser.setEmailVerified(true);
        keycloakUser.setEnabled("在职".equals(user.getStatus()));
        
        // 设置名称
        if (StringUtils.hasText(user.getUserName())) {
            keycloakUser.setFirstName(user.getUserName());
        }
        
        // 设置自定义属性
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("employee_id", Collections.singletonList(user.getEmployeeId()));
        attributes.put("department", Collections.singletonList(user.getDepartment()));
        attributes.put("user_type", Collections.singletonList(user.getUserType()));
        attributes.put("position", Collections.singletonList(user.getPosition()));
        keycloakUser.setAttributes(attributes);
        
        return keycloakUser;
    }
    
    /**
     * 设置用户密码
     */
    private void setUserPassword(RealmResource realm, String userId) {
        try {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(defaultPassword);
            credential.setTemporary(true); // 首次登录需要修改密码
            
            UserResource userResource = realm.users().get(userId);
            userResource.resetPassword(credential);
            log.debug("设置用户密码成功: {}", userId);
        } catch (Exception e) {
            log.error("设置用户密码失败: {}", userId, e);
        }
    }
    
    /**
     * 分配用户角色
     */
    private void assignUserRoles(RealmResource realm, String userId, String userType) {
        try {
            UserResource userResource = realm.users().get(userId);
            
            List<RoleRepresentation> rolesToAdd = new ArrayList<>();
            
            // 所有用户都有基础角色
            try {
                RoleRepresentation userRole = realm.roles().get("hkex-user").toRepresentation();
                if (userRole != null) {
                    rolesToAdd.add(userRole);
                }
            } catch (Exception e) {
                log.warn("角色 hkex-user 不存在");
            }
            
            // 主管有额外角色
            if ("主管".equals(userType)) {
                try {
                    RoleRepresentation managerRole = realm.roles().get("hkex-manager").toRepresentation();
                    if (managerRole != null) {
                        rolesToAdd.add(managerRole);
                    }
                } catch (Exception e) {
                    log.warn("角色 hkex-manager 不存在");
                }
            }
            
            if (!rolesToAdd.isEmpty()) {
                userResource.roles().realmLevel().add(rolesToAdd);
                log.debug("分配角色成功: {}", userId);
            }
        } catch (Exception e) {
            log.error("分配用户角色失败: {}", userId, e);
        }
    }
    
    /**
     * 从Response中获取创建的资源ID
     */
    private String getCreatedId(Response response) {
        URI location = response.getLocation();
        if (location != null) {
            String path = location.getPath();
            return path.substring(path.lastIndexOf('/') + 1);
        }
        return null;
    }
    
    /**
     * 根据JWT中的角色判断用户类型
     */
    private String determineUserType(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            Object rolesObj = realmAccess.get("roles");
            if (rolesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) rolesObj;
                if (roles.contains("hkex-manager")) {
                    return "主管";
                }
            }
        }
        return "员工";
    }
    
    /**
     * 手动同步单个用户
     */
    @Transactional
    public boolean syncUserToKeycloak(String email) {
        log.info("手动同步用户到Keycloak: {}", email);
        
        try {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (!userOpt.isPresent()) {
                log.error("用户不存在: {}", email);
                return false;
            }
            
            User user = userOpt.get();
            RealmResource realm = keycloakAdmin.realm(realmName);
            UsersResource usersResource = realm.users();
            
            // 检查用户是否已存在
            Optional<UserRepresentation> existingUser = findKeycloakUserByEmail(usersResource, email);
            
            if (existingUser.isPresent()) {
                // 更新现有用户
                updateKeycloakUser(realm, existingUser.get().getId(), user);
                user.setKeycloakId(existingUser.get().getId());
                userRepository.save(user);
                log.info("更新Keycloak用户成功: {}", email);
                return true;
            } else {
                // 创建新用户
                UserRepresentation keycloakUser = createKeycloakUser(user);
                Response response = usersResource.create(keycloakUser);
                
                if (response.getStatus() == 201) {
                    String userId = getCreatedId(response);
                    response.close();
                    
                    setUserPassword(realm, userId);
                    assignUserRoles(realm, userId, user.getUserType());
                    
                    user.setKeycloakId(userId);
                    userRepository.save(user);
                    
                    log.info("创建Keycloak用户成功: {}", email);
                    return true;
                } else {
                    log.error("创建Keycloak用户失败: {} - 状态码: {}", email, response.getStatus());
                    response.close();
                    return false;
                }
            }
            
        } catch (Exception e) {
            log.error("同步用户失败: {}", email, e);
            return false;
        }
    }
} 