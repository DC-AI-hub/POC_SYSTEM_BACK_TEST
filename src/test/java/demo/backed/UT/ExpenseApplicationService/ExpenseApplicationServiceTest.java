package demo.backed.UT.ExpenseApplicationService;

import demo.backed.BaseServiceTest;
import demo.backed.dto.*;
import demo.backed.entity.ApplicationStatus;
import demo.backed.entity.ExpenseApplication;
import demo.backed.entity.ExpenseItem;
import demo.backed.repository.ExpenseApplicationRepository;
import demo.backed.repository.ExpenseItemRepository;
import demo.backed.service.ExpenseApplicationService;
import demo.backed.service.UserService;
import demo.backed.service.WorkflowIntegrationService;
import demo.backed.util.ApplicationNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ExpenseApplicationService单元测试
 */
@DisplayName("费用申请服务测试")
class ExpenseApplicationServiceTest extends BaseServiceTest {

    @Mock
    private ExpenseApplicationRepository applicationRepository;

    @Mock
    private ExpenseItemRepository itemRepository;

    @Mock
    private UserService userService;

    @Mock
    private ApplicationNumberGenerator numberGenerator;

    @Mock
    private WorkflowIntegrationService workflowIntegrationService;

    @InjectMocks
    private ExpenseApplicationService expenseApplicationService;

    private ExpenseApplication testApplication;
    private ExpenseItem testItem;
    private CreateExpenseApplicationDTO testCreateDTO;
    private UserDTO testUser;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testApplication = createTestExpenseApplication();
        testItem = createTestExpenseItem();
        testCreateDTO = createTestCreateExpenseApplicationDTO();
        testUser = createTestUser();
    }

    // ==================== 创建费用申请测试 ====================

    @Test
    @DisplayName("应该成功创建费用申请")
    void shouldCreateApplicationSuccessfully() {
        // Given
        when(userService.getUserById(testCreateDTO.getApplicantId())).thenReturn(Optional.of(testUser));
        when(numberGenerator.generateExpenseNumber()).thenReturn("EXP-2025-123456");
        when(applicationRepository.save(any(ExpenseApplication.class))).thenReturn(testApplication);
        when(itemRepository.saveAll(anyList())).thenReturn(Arrays.asList(testItem));

        // When
        ExpenseApplicationDTO result = expenseApplicationService.createApplication(testCreateDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getApplicationNumber()).isEqualTo(testApplication.getApplicationNumber());
        assertThat(result.getApplicantName()).isEqualTo(testUser.getUserName());
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
        
        verify(userService).getUserById(testCreateDTO.getApplicantId());
        verify(numberGenerator).generateExpenseNumber();
        verify(applicationRepository).save(any(ExpenseApplication.class));
        verify(itemRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("创建费用申请时申请人不存在应该抛出异常")
    void shouldThrowExceptionWhenApplicantNotFound() {
        // Given
        when(userService.getUserById(testCreateDTO.getApplicantId())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> expenseApplicationService.createApplication(testCreateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("申请人不存在");
        
        verify(userService).getUserById(testCreateDTO.getApplicantId());
        verify(applicationRepository, never()).save(any(ExpenseApplication.class));
    }

    @Test
    @DisplayName("创建费用申请时验证失败应该抛出异常")
    void shouldThrowExceptionWhenValidationFails() {
        // Given
        CreateExpenseApplicationDTO invalidDto = createTestCreateExpenseApplicationDTO();
        invalidDto.setItems(new ArrayList<>()); // 创建空的可变列表，触发验证失败
        
        // When & Then
        assertThatThrownBy(() -> expenseApplicationService.createApplication(invalidDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("申请总金额必须大于0");
    }

    // ==================== 提交审批测试 ====================

    @Test
    @DisplayName("应该成功提交费用申请审批")
    void shouldSubmitForApprovalSuccessfully() {
        // Given
        Long applicationId = createTestApplicationId();
        testApplication.setStatus(ApplicationStatus.DRAFT);
        testApplication.setTotalAmount(new BigDecimal("1000.00"));
        // 设置items属性以通过验证
        testApplication.setItems(Arrays.asList(testItem));
        
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(testApplication));
        when(workflowIntegrationService.startExpenseApprovalWorkflow(testApplication)).thenReturn("workflow-123");
        when(applicationRepository.save(any(ExpenseApplication.class))).thenReturn(testApplication);
        when(itemRepository.findByApplicationIdOrderBySortOrder(applicationId)).thenReturn(Arrays.asList(testItem));

        // When
        ExpenseApplicationDTO result = expenseApplicationService.submitForApproval(applicationId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(result.getWorkflowInstanceId()).isEqualTo("workflow-123");
        
        verify(applicationRepository).findById(applicationId);
        verify(workflowIntegrationService).startExpenseApprovalWorkflow(testApplication);
        verify(applicationRepository).save(any(ExpenseApplication.class));
    }

    @Test
    @DisplayName("提交审批时申请不存在应该抛出异常")
    void shouldThrowExceptionWhenApplicationNotFoundForSubmit() {
        // Given
        Long applicationId = createTestApplicationId();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> expenseApplicationService.submitForApproval(applicationId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("申请单不存在");
        
        verify(applicationRepository).findById(applicationId);
        verify(workflowIntegrationService, never()).startExpenseApprovalWorkflow(any());
    }

    @Test
    @DisplayName("提交审批时状态不允许应该抛出异常")
    void shouldThrowExceptionWhenStatusNotAllowSubmit() {
        // Given
        Long applicationId = createTestApplicationId();
        testApplication.setStatus(ApplicationStatus.APPROVED); // 已审批状态不允许再次提交
        
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(testApplication));

        // When & Then
        assertThatThrownBy(() -> expenseApplicationService.submitForApproval(applicationId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("当前状态不允许提交审批");
        
        verify(applicationRepository).findById(applicationId);
        verify(workflowIntegrationService, never()).startExpenseApprovalWorkflow(any());
    }

    // ==================== 更新申请状态测试 ====================

    @Test
    @DisplayName("应该成功更新申请状态")
    void shouldUpdateApplicationStatusSuccessfully() {
        // Given
        String workflowInstanceId = "workflow-123";
        ApplicationStatus newStatus = ApplicationStatus.APPROVED;
        String comment = "审批通过";
        
        when(applicationRepository.findByWorkflowInstanceId(workflowInstanceId)).thenReturn(Optional.of(testApplication));
        when(applicationRepository.save(any(ExpenseApplication.class))).thenReturn(testApplication);

        // When
        expenseApplicationService.updateApplicationStatus(workflowInstanceId, newStatus, comment);

        // Then
        verify(applicationRepository).findByWorkflowInstanceId(workflowInstanceId);
        verify(applicationRepository).save(any(ExpenseApplication.class));
    }

    @Test
    @DisplayName("更新申请状态时工作流实例不存在应该抛出异常")
    void shouldThrowExceptionWhenWorkflowInstanceNotFound() {
        // Given
        String workflowInstanceId = "non-existent-workflow";
        ApplicationStatus newStatus = ApplicationStatus.APPROVED;
        String comment = "审批通过";
        
        when(applicationRepository.findByWorkflowInstanceId(workflowInstanceId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> expenseApplicationService.updateApplicationStatus(workflowInstanceId, newStatus, comment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("找不到对应的申请单");
        
        verify(applicationRepository).findByWorkflowInstanceId(workflowInstanceId);
        verify(applicationRepository, never()).save(any(ExpenseApplication.class));
    }

    // ==================== 查询费用申请测试 ====================

    @Test
    @DisplayName("应该成功分页查询费用申请")
    @SuppressWarnings("unchecked")
    void shouldFindApplicationsSuccessfully() {
        // Given
        ExpenseQueryDTO queryDto = new ExpenseQueryDTO();
        queryDto.setApplicantId(1L);
        queryDto.setStatus(ApplicationStatus.DRAFT);
        
        Pageable pageable = PageRequest.of(0, 10);
        Page<ExpenseApplication> applicationPage = new PageImpl<>(Arrays.asList(testApplication));
        
        when(applicationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(applicationPage);
        when(itemRepository.findByApplicationIdOrderBySortOrder(testApplication.getId())).thenReturn(Arrays.asList(testItem));

        // When
        Page<ExpenseApplicationDTO> result = expenseApplicationService.findApplications(queryDto, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(testApplication.getId());
        
        verify(applicationRepository).findAll(any(Specification.class), eq(pageable));
        verify(itemRepository).findByApplicationIdOrderBySortOrder(testApplication.getId());
    }

    @Test
    @DisplayName("应该成功获取申请单详情")
    void shouldGetApplicationDetailSuccessfully() {
        // Given
        Long applicationId = createTestApplicationId();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(testApplication));
        when(itemRepository.findByApplicationIdOrderBySortOrder(applicationId)).thenReturn(Arrays.asList(testItem));

        // When
        ExpenseApplicationDTO result = expenseApplicationService.getApplicationDetail(applicationId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testApplication.getId());
        assertThat(result.getItems()).hasSize(1);
        
        verify(applicationRepository).findById(applicationId);
        verify(itemRepository).findByApplicationIdOrderBySortOrder(applicationId);
    }

    @Test
    @DisplayName("获取申请单详情时申请不存在应该抛出异常")
    void shouldThrowExceptionWhenApplicationNotFoundForDetail() {
        // Given
        Long applicationId = createTestApplicationId();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> expenseApplicationService.getApplicationDetail(applicationId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("申请单不存在");
        
        verify(applicationRepository).findById(applicationId);
        verify(itemRepository, never()).findByApplicationIdOrderBySortOrder(any());
    }

    // ==================== 更新费用申请测试 ====================

    @Test
    @DisplayName("应该成功更新费用申请")
    void shouldUpdateApplicationSuccessfully() {
        // Given
        Long applicationId = createTestApplicationId();
        testApplication.setStatus(ApplicationStatus.DRAFT);
        
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(testApplication));
        when(applicationRepository.save(any(ExpenseApplication.class))).thenReturn(testApplication);
        when(itemRepository.saveAll(anyList())).thenReturn(Arrays.asList(testItem));
        doNothing().when(itemRepository).deleteByApplicationId(applicationId);

        // When
        ExpenseApplicationDTO result = expenseApplicationService.updateApplication(applicationId, testCreateDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testApplication.getId());
        
        verify(applicationRepository).findById(applicationId);
        verify(itemRepository).deleteByApplicationId(applicationId);
        verify(itemRepository).saveAll(anyList());
        verify(applicationRepository).save(any(ExpenseApplication.class));
    }

    @Test
    @DisplayName("更新费用申请时申请不存在应该抛出异常")
    void shouldThrowExceptionWhenApplicationNotFoundForUpdate() {
        // Given
        Long applicationId = createTestApplicationId();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> expenseApplicationService.updateApplication(applicationId, testCreateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("申请单不存在");
        
        verify(applicationRepository).findById(applicationId);
        verify(applicationRepository, never()).save(any(ExpenseApplication.class));
    }

    @Test
    @DisplayName("更新费用申请时状态不允许编辑应该抛出异常")
    void shouldThrowExceptionWhenStatusNotAllowEdit() {
        // Given
        Long applicationId = createTestApplicationId();
        testApplication.setStatus(ApplicationStatus.APPROVED); // 已审批状态不允许编辑
        
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(testApplication));

        // When & Then
        assertThatThrownBy(() -> expenseApplicationService.updateApplication(applicationId, testCreateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("当前状态不允许编辑");
        
        verify(applicationRepository).findById(applicationId);
        verify(applicationRepository, never()).save(any(ExpenseApplication.class));
    }

    // ==================== 删除费用申请测试 ====================

    @Test
    @DisplayName("应该成功删除费用申请")
    void shouldDeleteApplicationSuccessfully() {
        // Given
        Long applicationId = createTestApplicationId();
        testApplication.setStatus(ApplicationStatus.DRAFT);
        
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(testApplication));
        doNothing().when(itemRepository).deleteByApplicationId(applicationId);
        doNothing().when(applicationRepository).deleteById(applicationId);

        // When
        expenseApplicationService.deleteApplication(applicationId);

        // Then
        verify(applicationRepository).findById(applicationId);
        verify(itemRepository).deleteByApplicationId(applicationId);
        verify(applicationRepository).deleteById(applicationId);
    }

    @Test
    @DisplayName("删除费用申请时申请不存在应该抛出异常")
    void shouldThrowExceptionWhenApplicationNotFoundForDelete() {
        // Given
        Long applicationId = createTestApplicationId();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> expenseApplicationService.deleteApplication(applicationId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("申请单不存在");
        
        verify(applicationRepository).findById(applicationId);
        verify(applicationRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("删除费用申请时状态不允许删除应该抛出异常")
    void shouldThrowExceptionWhenStatusNotAllowDelete() {
        // Given
        Long applicationId = createTestApplicationId();
        testApplication.setStatus(ApplicationStatus.APPROVED); // 已审批状态不允许删除
        
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(testApplication));

        // When & Then
        assertThatThrownBy(() -> expenseApplicationService.deleteApplication(applicationId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("当前状态不允许删除");
        
        verify(applicationRepository).findById(applicationId);
        verify(applicationRepository, never()).deleteById(any());
    }

    // ==================== 测试数据创建方法 ====================

    /**
     * 创建测试费用申请实体
     */
    private ExpenseApplication createTestExpenseApplication() {
        ExpenseApplication application = new ExpenseApplication();
        application.setId(createTestApplicationId());
        application.setApplicationNumber("EXP-2025-123456");
        application.setApplicantId(1L);
        application.setApplicantName("张三");
        application.setDepartment("技术部");
        application.setCompany("港交所科技有限公司");
        application.setApplyDate(LocalDate.now());
        application.setDescription("参加北京技术交流会议相关费用");
        application.setTotalAmount(new BigDecimal("1000.00"));
        application.setCurrency("CNY");
        application.setStatus(ApplicationStatus.DRAFT);
        application.setCreatedTime(LocalDateTime.now());
        application.setUpdatedTime(LocalDateTime.now());
        return application;
    }

    /**
     * 创建测试费用明细实体
     */
    private ExpenseItem createTestExpenseItem() {
        ExpenseItem item = new ExpenseItem();
        item.setId(1L);
        item.setApplicationId(createTestApplicationId());
        item.setExpenseCategory("交通费");
        item.setPurpose("往返机票");
        item.setAmount(new BigDecimal("800.00"));
        item.setExpenseDate(LocalDate.now());
        item.setRemark("北京-深圳往返");
        item.setSortOrder(0);
        item.setReceiptRequired(true);
        item.setCreatedTime(LocalDateTime.now());
        item.setUpdatedTime(LocalDateTime.now());
        return item;
    }

    /**
     * 创建测试创建费用申请DTO
     */
    private CreateExpenseApplicationDTO createTestCreateExpenseApplicationDTO() {
        CreateExpenseApplicationDTO dto = new CreateExpenseApplicationDTO();
        dto.setApplicantId(1L);
        dto.setCompany("港交所科技有限公司");
        dto.setApplyDate(LocalDate.now());
        dto.setDescription("参加北京技术交流会议相关费用");
        dto.setCurrency("CNY");
        
        // 创建费用明细
        ExpenseItemDTO itemDto = new ExpenseItemDTO();
        itemDto.setExpenseCategory("交通费");
        itemDto.setPurpose("往返机票");
        itemDto.setAmount(new BigDecimal("800.00"));
        itemDto.setExpenseDate(LocalDate.now());
        itemDto.setRemark("北京-深圳往返");
        itemDto.setReceiptRequired(true);
        
        dto.setItems(Arrays.asList(itemDto));
        return dto;
    }

    /**
     * 创建测试用户DTO
     */
    private UserDTO createTestUser() {
        UserDTO user = new UserDTO();
        user.setId(1L);
        user.setUserName("张三");
        user.setEmployeeId("EMP001");
        user.setEmail("zhangsan@example.com");
        user.setDepartment("技术部");
        user.setPosition("高级工程师");
        user.setStatus("在职");
        return user;
    }

    /**
     * 创建测试申请ID
     */
    protected Long createTestApplicationId() {
        return 1L;
    }
} 