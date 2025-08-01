package demo.backed.UT.KeycloakUserSyncService;

import demo.backed.BaseServiceTest;
import demo.backed.entity.User;
import demo.backed.repository.UserRepository;
import demo.backed.service.KeycloakUserSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * KeycloakUserSyncService单元测试
 */
@DisplayName("Keycloak用户同步服务测试")
class KeycloakUserSyncServiceTest extends BaseServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Keycloak keycloakAdmin;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @Mock
    private RolesResource rolesResource;

    @Mock
    private RoleResource roleResource;
    
    @Mock
    private org.keycloak.admin.client.resource.RoleMappingResource roleMappingResource;
    
    @Mock
    private org.keycloak.admin.client.resource.RoleScopeResource roleScopeResource;

    @Mock
    private Response response;

    @InjectMocks
    private KeycloakUserSyncService keycloakUserSyncService;

    private User testUser;
    private UserRepresentation testKeycloakUser;
    private Jwt testJwt;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testUser = createTestUser();
        testKeycloakUser = createTestKeycloakUser();
        testJwt = createTestJwt();
        
        // 设置配置属性
        ReflectionTestUtils.setField(keycloakUserSyncService, "realmName", "test-realm");
        ReflectionTestUtils.setField(keycloakUserSyncService, "syncEnabled", true);
        ReflectionTestUtils.setField(keycloakUserSyncService, "defaultPassword", "Hkex@2024");
    }

    // ==================== 同步现有用户到Keycloak测试 ====================

    @Test
    @DisplayName("应该成功同步现有用户到Keycloak")
    void shouldSyncExistingUsersToKeycloakSuccessfully() throws Exception {
        // Given
        List<User> activeUsers = Arrays.asList(testUser);
        when(keycloakAdmin.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(realmResource.roles()).thenReturn(rolesResource);
        when(userRepository.findByStatus("在职")).thenReturn(activeUsers);
        when(usersResource.search(testUser.getEmail(), true)).thenReturn(new ArrayList<>());
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getLocation()).thenReturn(URI.create("http://keycloak/users/test-id"));
        when(usersResource.get("test-id")).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
        when(rolesResource.get("hkex-user")).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(createTestRole("hkex-user"));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        keycloakUserSyncService.syncExistingUsersToKeycloak();

        // Then
        verify(userRepository).findByStatus("在职");
        verify(usersResource).search(testUser.getEmail(), true);
        verify(usersResource).create(any(UserRepresentation.class));
        verify(userResource).resetPassword(any());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("同步时跳过没有邮箱的用户")
    void shouldSkipUsersWithoutEmail() {
        // Given
        User userWithoutEmail = createTestUser();
        userWithoutEmail.setEmail(null);
        List<User> activeUsers = Arrays.asList(userWithoutEmail);
        when(keycloakAdmin.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(userRepository.findByStatus("在职")).thenReturn(activeUsers);

        // When
        keycloakUserSyncService.syncExistingUsersToKeycloak();

        // Then
        verify(userRepository).findByStatus("在职");
        verify(usersResource, never()).create(any(UserRepresentation.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("同步时更新已存在的Keycloak用户")
    void shouldUpdateExistingKeycloakUser() {
        // Given
        List<User> activeUsers = Arrays.asList(testUser);
        testKeycloakUser.setId("existing-id");
        when(keycloakAdmin.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(userRepository.findByStatus("在职")).thenReturn(activeUsers);
        when(usersResource.search(testUser.getEmail(), true)).thenReturn(Arrays.asList(testKeycloakUser));
        when(usersResource.get("existing-id")).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(testKeycloakUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        keycloakUserSyncService.syncExistingUsersToKeycloak();

        // Then
        verify(userRepository).findByStatus("在职");
        verify(usersResource).search(testUser.getEmail(), true);
        verify(userResource).update(any(UserRepresentation.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("同步禁用时应该跳过")
    void shouldSkipSyncWhenDisabled() {
        // Given
        ReflectionTestUtils.setField(keycloakUserSyncService, "syncEnabled", false);

        // When
        keycloakUserSyncService.syncExistingUsersToKeycloak();

        // Then
        verify(userRepository, never()).findByStatus(any());
        verify(usersResource, never()).create(any(UserRepresentation.class));
    }

    // ==================== Keycloak用户同步到本地测试 ====================

    @Test
    @DisplayName("应该成功同步Keycloak用户到本地")
    void shouldSyncKeycloakUserToLocalSuccessfully() {
        // Given
        when(userRepository.findByEmail(testJwt.getClaimAsString("email"))).thenReturn(Optional.empty());
        when(userRepository.findByKeycloakId(testJwt.getSubject())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = keycloakUserSyncService.syncKeycloakUserToLocal(testJwt);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(testJwt.getClaimAsString("email"));
        assertThat(result.getKeycloakId()).isEqualTo(testJwt.getSubject());
        
        verify(userRepository).findByEmail(testJwt.getClaimAsString("email"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("应该通过邮箱找到现有用户并更新")
    void shouldFindExistingUserByEmailAndUpdate() {
        // Given
        testUser.setKeycloakId("old-id");
        when(userRepository.findByEmail(testJwt.getClaimAsString("email"))).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = keycloakUserSyncService.syncKeycloakUserToLocal(testJwt);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getKeycloakId()).isEqualTo(testJwt.getSubject());
        
        verify(userRepository).findByEmail(testJwt.getClaimAsString("email"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("应该通过Keycloak ID找到现有用户")
    void shouldFindExistingUserByKeycloakId() {
        // Given
        when(userRepository.findByEmail(testJwt.getClaimAsString("email"))).thenReturn(Optional.empty());
        when(userRepository.findByKeycloakId(testJwt.getSubject())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = keycloakUserSyncService.syncKeycloakUserToLocal(testJwt);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getKeycloakId()).isEqualTo(testJwt.getSubject());
        
        verify(userRepository).findByEmail(testJwt.getClaimAsString("email"));
        verify(userRepository).findByKeycloakId(testJwt.getSubject());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("同步Keycloak用户时邮箱为空应该抛出异常")
    void shouldThrowExceptionWhenEmailIsEmpty() {
        // Given
        Jwt jwtWithoutEmail = createTestJwtWithoutEmail();

        // When & Then
        assertThatThrownBy(() -> keycloakUserSyncService.syncKeycloakUserToLocal(jwtWithoutEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户邮箱不能为空");
        
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("应该根据JWT中的角色确定用户类型")
    void shouldDetermineUserTypeFromJwtRoles() {
        // Given
        Jwt managerJwt = createTestJwtWithManagerRole();
        when(userRepository.findByEmail(managerJwt.getClaimAsString("email"))).thenReturn(Optional.empty());
        when(userRepository.findByKeycloakId(managerJwt.getSubject())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = keycloakUserSyncService.syncKeycloakUserToLocal(managerJwt);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(argThat(user -> "主管".equals(user.getUserType())));
    }

    // ==================== 手动同步单个用户测试 ====================

    @Test
    @DisplayName("应该成功手动同步用户到Keycloak")
    void shouldSyncUserToKeycloakManuallySuccessfully() throws Exception {
        // Given
        String email = testUser.getEmail();
        when(keycloakAdmin.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(realmResource.roles()).thenReturn(rolesResource);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(usersResource.search(email, true)).thenReturn(new ArrayList<>());
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getLocation()).thenReturn(URI.create("http://keycloak/users/test-id"));
        when(usersResource.get("test-id")).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
        when(rolesResource.get("hkex-user")).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(createTestRole("hkex-user"));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        boolean result = keycloakUserSyncService.syncUserToKeycloak(email);

        // Then
        assertThat(result).isTrue();
        verify(userRepository).findByEmail(email);
        verify(usersResource).create(any(UserRepresentation.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("手动同步不存在的用户应该返回false")
    void shouldReturnFalseWhenSyncNonExistentUser() {
        // Given
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When
        boolean result = keycloakUserSyncService.syncUserToKeycloak(email);

        // Then
        assertThat(result).isFalse();
        verify(userRepository).findByEmail(email);
        verify(usersResource, never()).create(any(UserRepresentation.class));
    }

    @Test
    @DisplayName("手动同步时更新已存在的Keycloak用户")
    void shouldUpdateExistingKeycloakUserWhenSyncManually() {
        // Given
        String email = testUser.getEmail();
        testKeycloakUser.setId("existing-id");
        when(keycloakAdmin.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(usersResource.search(email, true)).thenReturn(Arrays.asList(testKeycloakUser));
        when(usersResource.get("existing-id")).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(testKeycloakUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        boolean result = keycloakUserSyncService.syncUserToKeycloak(email);

        // Then
        assertThat(result).isTrue();
        verify(userRepository).findByEmail(email);
        verify(userResource).update(any(UserRepresentation.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("手动同步时创建Keycloak用户失败应该返回false")
    void shouldReturnFalseWhenCreateKeycloakUserFails() throws Exception {
        // Given
        String email = testUser.getEmail();
        when(keycloakAdmin.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(usersResource.search(email, true)).thenReturn(new ArrayList<>());
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(400); // 创建失败

        // When
        boolean result = keycloakUserSyncService.syncUserToKeycloak(email);

        // Then
        assertThat(result).isFalse();
        verify(userRepository).findByEmail(email);
        verify(usersResource).create(any(UserRepresentation.class));
        verify(response).close();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("手动同步时发生异常应该返回false")
    void shouldReturnFalseWhenExceptionOccursDuringManualSync() {
        // Given
        String email = testUser.getEmail();
        when(keycloakAdmin.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(usersResource.search(email, true)).thenThrow(new RuntimeException("Keycloak error"));

        // When
        boolean result = keycloakUserSyncService.syncUserToKeycloak(email);

        // Then
        assertThat(result).isFalse();
        verify(userRepository).findByEmail(email);
        verify(usersResource).search(email, true);
    }

    // ==================== 测试数据创建方法 ====================

    /**
     * 创建测试用户实体
     */
    private User createTestUser() {
        User user = new User();
        user.setId(createTestUserId());
        user.setUserName("张三");
        user.setEmployeeId("EMP001");
        user.setEmail("zhangsan@example.com");
        user.setPhone("13800138000");
        user.setDepartment("技术部");
        user.setPosition("高级工程师");
        user.setUserType("员工");
        user.setStatus("在职");
        user.setKeycloakId("test-keycloak-id");
        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());
        return user;
    }

    /**
     * 创建测试Keycloak用户表示
     */
    private UserRepresentation createTestKeycloakUser() {
        UserRepresentation user = new UserRepresentation();
        user.setId("test-keycloak-id");
        user.setUsername("zhangsan@example.com");
        user.setEmail("zhangsan@example.com");
        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setFirstName("张三");
        
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("employee_id", Collections.singletonList("EMP001"));
        attributes.put("department", Collections.singletonList("技术部"));
        attributes.put("user_type", Collections.singletonList("员工"));
        attributes.put("position", Collections.singletonList("高级工程师"));
        user.setAttributes(attributes);
        
        return user;
    }

    /**
     * 创建测试JWT
     */
    private Jwt createTestJwt() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        headers.put("typ", "JWT");
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "test-keycloak-id");
        claims.put("email", "zhangsan@example.com");
        claims.put("preferred_username", "zhangsan");
        claims.put("name", "张三");
        claims.put("employee_id", "EMP001");
        claims.put("department", "技术部");
        claims.put("user_type", "员工");
        claims.put("position", "高级工程师");
        
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", Arrays.asList("hkex-user"));
        claims.put("realm_access", realmAccess);
        
        return new Jwt("test-token", Instant.now(), Instant.now().plusSeconds(3600), headers, claims);
    }

    /**
     * 创建没有邮箱的测试JWT
     */
    private Jwt createTestJwtWithoutEmail() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        headers.put("typ", "JWT");
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "test-keycloak-id");
        claims.put("preferred_username", "zhangsan");
        claims.put("name", "张三");
        
        return new Jwt("test-token", Instant.now(), Instant.now().plusSeconds(3600), headers, claims);
    }

    /**
     * 创建包含主管角色的测试JWT
     */
    private Jwt createTestJwtWithManagerRole() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        headers.put("typ", "JWT");
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "test-keycloak-id");
        claims.put("email", "manager@example.com");
        claims.put("preferred_username", "manager");
        claims.put("name", "经理");
        
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", Arrays.asList("hkex-user", "hkex-manager"));
        claims.put("realm_access", realmAccess);
        
        return new Jwt("test-token", Instant.now(), Instant.now().plusSeconds(3600), headers, claims);
    }

    /**
     * 创建测试角色
     */
    private RoleRepresentation createTestRole(String roleName) {
        RoleRepresentation role = new RoleRepresentation();
        role.setId("role-" + roleName);
        role.setName(roleName);
        role.setDescription("Test role: " + roleName);
        return role;
    }

    /**
     * 创建测试用户ID
     */
    protected Long createTestUserId() {
        return 1L;
    }
} 