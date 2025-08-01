package demo.backed.UT.UserService;

import demo.backed.BaseServiceTest;
import demo.backed.dto.LoginRequest;
import demo.backed.dto.LoginResponse;
import demo.backed.dto.UserDTO;
import demo.backed.entity.User;
import demo.backed.repository.UserRepository;
import demo.backed.service.OrganizationService;
import demo.backed.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserService单元测试
 */
@DisplayName("用户服务测试")
class UserServiceTest extends BaseServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationService organizationService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testUser = createTestUser();
        testUserDTO = createTestUserDTO();
    }

    // ===== 基础CRUD测试 =====
    
    @Test
    @DisplayName("应该成功获取用户列表")
    void shouldGetUsersSuccessfully() {
        // Given
        List<User> users = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(users);
        
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        // When
        Page<UserDTO> result = userService.getUsers(0, 10, null, null, null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUserName()).isEqualTo(testUser.getUserName());
        verify(userRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("应该支持条件查询用户列表")
    void shouldGetUsersWithConditions() {
        // Given
        List<User> users = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(users);
        
        when(userRepository.findByConditions(eq("测试"), eq("技术部"), eq("在职"), eq("员工"), any(Pageable.class)))
                .thenReturn(userPage);

        // When
        Page<UserDTO> result = userService.getUsers(0, 10, "测试", "技术部", "在职", "员工");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findByConditions(eq("测试"), eq("技术部"), eq("在职"), eq("员工"), any(Pageable.class));
    }

    @Test
    @DisplayName("应该根据ID成功获取用户")
    void shouldGetUserByIdSuccessfully() {
        // Given
        Long userId = createTestUserId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        Optional<UserDTO> result = userService.getUserById(userId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUserName()).isEqualTo(testUser.getUserName());
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("应该根据工号成功获取用户")
    void shouldGetUserByEmployeeIdSuccessfully() {
        // Given
        when(userRepository.findByEmployeeId("EMP567")).thenReturn(Optional.of(testUser));

        // When
        Optional<UserDTO> result = userService.getUserByEmployeeId("EMP567");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmployeeId()).isEqualTo("EMP567");
        verify(userRepository).findByEmployeeId("EMP567");
    }

    @Test
    @DisplayName("应该根据邮箱成功获取用户")
    void shouldGetUserByEmailSuccessfully() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        Optional<UserDTO> result = userService.getUserByEmail("test@example.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("应该成功创建用户")
    void shouldCreateUserSuccessfully() {
        // Given
        when(userRepository.countByEmployeeIdAndIdNot(testUserDTO.getEmployeeId(), null)).thenReturn(0L);
        when(userRepository.countByEmailAndIdNot(testUserDTO.getEmail(), null)).thenReturn(0L);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(organizationService).clearOrganizationTreeCache();

        // When
        UserDTO result = userService.createUser(testUserDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserName()).isEqualTo(testUserDTO.getUserName());
        verify(userRepository).save(any(User.class));
        verify(organizationService).clearOrganizationTreeCache();
    }

    @Test
    @DisplayName("创建用户时工号重复应该抛出异常")
    void shouldThrowExceptionWhenEmployeeIdDuplicate() {
        // Given
        when(userRepository.countByEmployeeIdAndIdNot(testUserDTO.getEmployeeId(), null)).thenReturn(1L);

        // When & Then
        assertThatThrownBy(() -> userService.createUser(testUserDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("工号已存在");
        
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("创建用户时邮箱重复应该抛出异常")
    void shouldThrowExceptionWhenEmailDuplicate() {
        // Given
        when(userRepository.countByEmployeeIdAndIdNot(testUserDTO.getEmployeeId(), null)).thenReturn(0L);
        when(userRepository.countByEmailAndIdNot(testUserDTO.getEmail(), null)).thenReturn(1L);

        // When & Then
        assertThatThrownBy(() -> userService.createUser(testUserDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("邮箱已存在");
        
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("创建用户时必填字段为空应该抛出异常")
    void shouldThrowExceptionWhenRequiredFieldsEmpty() {
        // Given
        UserDTO invalidUser = new UserDTO();
        invalidUser.setUserName(""); // 空姓名

        // When & Then
        assertThatThrownBy(() -> userService.createUser(invalidUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("工号不能为空");
    }

    @Test
    @DisplayName("应该成功更新用户")
    void shouldUpdateUserSuccessfully() {
        // Given
        Long userId = createTestUserId();
        UserDTO updateDTO = new UserDTO();
        updateDTO.setUserName("更新后的姓名");
        updateDTO.setPhone("13900000000");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.countByEmployeeIdAndIdNot(testUser.getEmployeeId(), userId)).thenReturn(0L);
        when(userRepository.countByEmailAndIdNot(testUser.getEmail(), userId)).thenReturn(0L);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(organizationService).clearOrganizationTreeCache();

        // When
        UserDTO result = userService.updateUser(userId, updateDTO);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
        verify(organizationService).clearOrganizationTreeCache();
    }

    @Test
    @DisplayName("更新不存在的用户应该抛出异常")
    void shouldThrowExceptionWhenUpdateNonExistentUser() {
        // Given
        Long userId = createTestUserId();
        UserDTO updateDTO = new UserDTO();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(userId, updateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("用户不存在");
        
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("应该成功删除用户")
    void shouldDeleteUserSuccessfully() {
        // Given
        Long userId = createTestUserId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);
        doNothing().when(organizationService).clearOrganizationTreeCache();

        // When
        userService.deleteUser(userId);

        // Then
        verify(userRepository).findById(userId);
        verify(userRepository).delete(testUser);
        verify(organizationService).clearOrganizationTreeCache();
    }

    // ===== 登录相关测试 =====

    @Test
    @DisplayName("应该成功用户登录")
    void shouldLoginSuccessfully() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testUser.getEmail());
        loginRequest.setPassword("password123");
        
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = 
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        testUser.setPassword(encoder.encode("password123"));
        testUser.setStatus("在职");
        
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        LoginResponse result = userService.login(loginRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getToken()).isNotNull();
        assertThat(result.getUserInfo().getUserName()).isEqualTo(testUser.getUserName());
        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("用户不存在时登录应该抛出异常")
    void shouldThrowExceptionWhenLoginWithNonExistentUser() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nonexistent@example.com");
        loginRequest.setPassword("password123");
        
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    @DisplayName("用户状态异常时登录应该抛出异常")
    void shouldThrowExceptionWhenLoginWithInactiveUser() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testUser.getEmail());
        loginRequest.setPassword("password123");
        
        testUser.setStatus("离职");
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("用户状态异常，无法登录");
    }

    @Test
    @DisplayName("密码错误时登录应该抛出异常")
    void shouldThrowExceptionWhenLoginWithWrongPassword() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testUser.getEmail());
        loginRequest.setPassword("wrongpassword");
        
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = 
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        testUser.setPassword(encoder.encode("correctpassword"));
        testUser.setStatus("在职");
        
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("密码错误");
    }

    @Test
    @DisplayName("应该成功用户登出")
    void shouldLogoutSuccessfully() {
        // Given
        Long userId = createTestUserId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.logout(userId);

        // Then
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("应该成功更新用户登录信息")
    void shouldUpdateUserLoginInfoSuccessfully() {
        // Given
        Long userId = createTestUserId();
        LocalDateTime loginTime = LocalDateTime.now();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.updateUserLoginInfo(userId, true, loginTime);

        // Then
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("应该成功获取在线用户列表")
    void shouldGetOnlineUsersSuccessfully() {
        // Given
        testUser.setIsOnline(true);
        List<User> onlineUsers = Arrays.asList(testUser);
        when(userRepository.findByIsOnlineTrue()).thenReturn(onlineUsers);

        // When
        List<UserDTO> result = userService.getOnlineUsers();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserName()).isEqualTo(testUser.getUserName());
        verify(userRepository).findByIsOnlineTrue();
    }

    @Test
    @DisplayName("应该成功重置用户密码")
    void shouldResetPasswordSuccessfully() {
        // Given
        Long userId = createTestUserId();
        String newPassword = "newPassword123";
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.resetPassword(userId, newPassword);

        // Then
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
    }

    // ===== 查询和统计测试 =====

    @Test
    @DisplayName("应该根据部门和用户类型获取用户列表")
    void shouldGetUsersByDepartmentAndType() {
        // Given
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findByDepartmentAndUserType("技术部", "员工")).thenReturn(users);

        // When
        List<UserDTO> result = userService.getUsersByDepartmentAndType("技术部", "员工");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDepartment()).isEqualTo("技术部");
        verify(userRepository).findByDepartmentAndUserType("技术部", "员工");
    }

    @Test
    @DisplayName("应该根据职位获取用户列表")
    void shouldGetUsersByPosition() {
        // Given
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findByPosition("开发工程师")).thenReturn(users);

        // When
        List<UserDTO> result = userService.getUsersByPosition("开发工程师");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPosition()).isEqualTo("开发工程师");
        verify(userRepository).findByPosition("开发工程师");
    }

    @Test
    @DisplayName("应该获取部门用户统计")
    void shouldGetUserStatsByDepartment() {
        // Given
        Object[] stat1 = {"技术部", 10L};
        Object[] stat2 = {"市场部", 5L};
        List<Object[]> stats = Arrays.asList(stat1, stat2);
        when(userRepository.countUsersByDepartment()).thenReturn(stats);

        // When
        List<Object[]> result = userService.getUserStatsByDepartment();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0)[0]).isEqualTo("技术部");
        assertThat(result.get(0)[1]).isEqualTo(10L);
        verify(userRepository).countUsersByDepartment();
    }

    @Test
    @DisplayName("应该获取在线用户数量")
    void shouldGetOnlineUserCount() {
        // Given
        when(userRepository.countOnlineUsers()).thenReturn(5L);

        // When
        long result = userService.getOnlineUserCount();

        // Then
        assertThat(result).isEqualTo(5L);
        verify(userRepository).countOnlineUsers();
    }

    // ===== 数据验证测试 =====

    @Test
    @DisplayName("应该验证邮箱唯一性")
    void shouldCheckEmailUniqueness() {
        // Given
        when(userRepository.countByEmailAndIdNot("test@example.com", 1L)).thenReturn(0L);
        when(userRepository.countByEmailAndIdNot("duplicate@example.com", 1L)).thenReturn(1L);

        // When & Then
        assertThat(userService.isEmailUnique("test@example.com", 1L)).isTrue();
        assertThat(userService.isEmailUnique("duplicate@example.com", 1L)).isFalse();
        
        verify(userRepository).countByEmailAndIdNot("test@example.com", 1L);
        verify(userRepository).countByEmailAndIdNot("duplicate@example.com", 1L);
    }

    @Test
    @DisplayName("应该验证工号唯一性")
    void shouldCheckEmployeeIdUniqueness() {
        // Given
        when(userRepository.countByEmployeeIdAndIdNot("EMP001", 1L)).thenReturn(0L);
        when(userRepository.countByEmployeeIdAndIdNot("EMP002", 1L)).thenReturn(1L);

        // When & Then
        assertThat(userService.isEmployeeIdUnique("EMP001", 1L)).isTrue();
        assertThat(userService.isEmployeeIdUnique("EMP002", 1L)).isFalse();
        
        verify(userRepository).countByEmployeeIdAndIdNot("EMP001", 1L);
        verify(userRepository).countByEmployeeIdAndIdNot("EMP002", 1L);
    }

    // ===== CSV导入测试 =====

    @Test
    @DisplayName("CSV导入时应该处理数据验证错误")
    void shouldHandleValidationErrorsInCsvImport() throws Exception {
        // Given
        String csvContent = "姓名,工号,邮箱,部门,职位\n,EMP999,invalid-email,技术部,开发工程师"; // 缺少姓名，邮箱格式错误
        MockMultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv", csvContent.getBytes());

        // When
        Map<String, Object> result = userService.importUsersFromCsv(file);

        // Then
        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("successCount")).isEqualTo(0);
        // 验证错误数量（实际可能包含：姓名为空、邮箱格式错误、部门为空、工号为空等）
        assertThat((Integer) result.get("errorCount")).isGreaterThan(0);
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * 创建测试用户实体
     */
    private User createTestUser() {
        User user = new User();
        user.setId(createTestUserId());
        user.setUserName("测试用户");
        user.setEmployeeId("EMP567");
        user.setEmail("test@example.com");
        user.setPhone("13800138000");
        user.setDepartment("技术部");
        user.setPosition("开发工程师");
        user.setUserType("员工");
        user.setStatus("在职");
        user.setIsOnline(false);
        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());
        return user;
    }

    /**
     * 创建测试用户DTO
     */
    private UserDTO createTestUserDTO() {
        UserDTO dto = new UserDTO();
        dto.setUserName("测试用户");
        dto.setEmployeeId("EMP567");
        dto.setEmail("test@example.com");
        dto.setPhone("13800138000");
        dto.setDepartment("技术部");
        dto.setPosition("开发工程师");
        dto.setUserType("员工");
        dto.setStatus("在职");
        dto.setPassword("password123");
        return dto;
    }
} 