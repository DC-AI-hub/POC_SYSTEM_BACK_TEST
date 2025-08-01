package demo.backed.UT.WorkflowIntegrationService;

import demo.backed.BaseServiceTest;
import demo.backed.dto.*;
import demo.backed.entity.ExpenseApplication;
import demo.backed.service.WorkflowIntegrationService;
import demo.backed.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WorkflowIntegrationService单元测试
 */
@DisplayName("工作流集成服务测试")
class WorkflowIntegrationServiceTest extends BaseServiceTest {

    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private WorkflowIntegrationService workflowIntegrationService;

    private ExpenseApplication testApplication;
    private WorkflowInstanceDTO testWorkflowInstance;
    private PendingTaskDTO testPendingTask;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testApplication = createTestExpenseApplication();
        testWorkflowInstance = createTestWorkflowInstance();
        testPendingTask = createTestPendingTask();
    }

    // ==================== 启动工作流测试 ====================

    @Test
    @DisplayName("应该成功启动费用审批工作流")
    void shouldStartExpenseApprovalWorkflowSuccessfully() {
        // Given
        when(workflowService.startWorkflow(any(StartProcessRequest.class))).thenReturn(testWorkflowInstance);

        // When
        String result = workflowIntegrationService.startExpenseApprovalWorkflow(testApplication);

        // Then
        assertThat(result).isEqualTo(testWorkflowInstance.getProcessInstanceId());
        verify(workflowService).startWorkflow(argThat(request -> {
            assertThat(request.getBusinessType()).isEqualTo("EXPENSE");
            assertThat(request.getBusinessId()).isEqualTo(testApplication.getApplicationNumber());
            assertThat(request.getApplicantId()).isEqualTo(testApplication.getApplicantId());
            assertThat(request.getAmount()).isEqualTo(testApplication.getTotalAmount());
            assertThat(request.getTitle()).contains(testApplication.getApplicationNumber());
            assertThat(request.getVariables()).containsKey("applicationId");
            assertThat(request.getVariables()).containsKey("applicationNumber");
            return true;
        }));
    }

    @Test
    @DisplayName("启动工作流失败时应该抛出异常")
    void shouldThrowExceptionWhenStartWorkflowFails() {
        // Given
        when(workflowService.startWorkflow(any(StartProcessRequest.class)))
                .thenThrow(new RuntimeException("工作流引擎错误"));

        // When & Then
        assertThatThrownBy(() -> workflowIntegrationService.startExpenseApprovalWorkflow(testApplication))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("启动工作流失败");
        
        verify(workflowService).startWorkflow(any(StartProcessRequest.class));
    }

    // ==================== 获取任务列表测试 ====================

    @Test
    @DisplayName("应该成功获取用户待办任务列表")
    void shouldGetPendingTasksSuccessfully() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<PendingTaskDTO> expectedPage = new PageImpl<>(Arrays.asList(testPendingTask));
        when(workflowService.getPendingTasks(userId, pageable)).thenReturn(expectedPage);

        // When
        Page<PendingTaskDTO> result = workflowIntegrationService.getPendingTasks(userId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testPendingTask);
        verify(workflowService).getPendingTasks(userId, pageable);
    }

    @Test
    @DisplayName("获取待办任务失败时应该抛出异常")
    void shouldThrowExceptionWhenGetPendingTasksFails() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        when(workflowService.getPendingTasks(userId, pageable))
                .thenThrow(new RuntimeException("数据库连接错误"));

        // When & Then
        assertThatThrownBy(() -> workflowIntegrationService.getPendingTasks(userId, pageable))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("获取待办任务失败");
        
        verify(workflowService).getPendingTasks(userId, pageable);
    }

    @Test
    @DisplayName("应该成功获取用户已办任务列表")
    void shouldGetHandledTasksSuccessfully() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<PendingTaskDTO> expectedPage = new PageImpl<>(Arrays.asList(testPendingTask));
        when(workflowService.getHandledTasks(userId, pageable)).thenReturn(expectedPage);

        // When
        Page<PendingTaskDTO> result = workflowIntegrationService.getHandledTasks(userId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testPendingTask);
        verify(workflowService).getHandledTasks(userId, pageable);
    }

    @Test
    @DisplayName("获取已办任务失败时应该抛出异常")
    void shouldThrowExceptionWhenGetHandledTasksFails() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        when(workflowService.getHandledTasks(userId, pageable))
                .thenThrow(new RuntimeException("服务不可用"));

        // When & Then
        assertThatThrownBy(() -> workflowIntegrationService.getHandledTasks(userId, pageable))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("获取已办任务失败");
        
        verify(workflowService).getHandledTasks(userId, pageable);
    }

    // ==================== 审批操作测试 ====================

    @Test
    @DisplayName("应该成功审批通过")
    void shouldApproveTaskSuccessfully() {
        // Given
        Long instanceId = 1L;
        String taskId = "task-123";
        String userId = "user-1";
        String comment = "审批通过";
        doNothing().when(workflowService).approve(instanceId, taskId, comment);

        // When
        workflowIntegrationService.approveTask(instanceId, taskId, userId, comment);

        // Then
        verify(workflowService).approve(instanceId, taskId, comment);
    }

    @Test
    @DisplayName("审批通过失败时应该抛出异常")
    void shouldThrowExceptionWhenApproveFails() {
        // Given
        Long instanceId = 1L;
        String taskId = "task-123";
        String userId = "user-1";
        String comment = "审批通过";
        doThrow(new RuntimeException("任务不存在")).when(workflowService).approve(instanceId, taskId, comment);

        // When & Then
        assertThatThrownBy(() -> workflowIntegrationService.approveTask(instanceId, taskId, userId, comment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("审批失败");
        
        verify(workflowService).approve(instanceId, taskId, comment);
    }

    @Test
    @DisplayName("应该成功审批拒绝")
    void shouldRejectTaskSuccessfully() {
        // Given
        Long instanceId = 1L;
        String taskId = "task-123";
        String userId = "user-1";
        String comment = "费用不合理";
        doNothing().when(workflowService).reject(instanceId, taskId, comment);

        // When
        workflowIntegrationService.rejectTask(instanceId, taskId, userId, comment);

        // Then
        verify(workflowService).reject(instanceId, taskId, comment);
    }

    @Test
    @DisplayName("拒绝审批时意见为空应该抛出异常")
    void shouldThrowExceptionWhenRejectWithEmptyComment() {
        // Given
        Long instanceId = 1L;
        String taskId = "task-123";
        String userId = "user-1";
        String comment = "";

        // When & Then
        assertThatThrownBy(() -> workflowIntegrationService.rejectTask(instanceId, taskId, userId, comment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("拒绝审批必须填写意见");
        
        verify(workflowService, never()).reject(any(), any(), any());
    }

    @Test
    @DisplayName("拒绝审批时意见为null应该抛出异常")
    void shouldThrowExceptionWhenRejectWithNullComment() {
        // Given
        Long instanceId = 1L;
        String taskId = "task-123";
        String userId = "user-1";
        String comment = null;

        // When & Then
        assertThatThrownBy(() -> workflowIntegrationService.rejectTask(instanceId, taskId, userId, comment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("拒绝审批必须填写意见");
        
        verify(workflowService, never()).reject(any(), any(), any());
    }

    @Test
    @DisplayName("审批拒绝失败时应该抛出异常")
    void shouldThrowExceptionWhenRejectFails() {
        // Given
        Long instanceId = 1L;
        String taskId = "task-123";
        String userId = "user-1";
        String comment = "费用不合理";
        doThrow(new RuntimeException("工作流错误")).when(workflowService).reject(instanceId, taskId, comment);

        // When & Then
        assertThatThrownBy(() -> workflowIntegrationService.rejectTask(instanceId, taskId, userId, comment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("拒绝审批失败");
        
        verify(workflowService).reject(instanceId, taskId, comment);
    }

    // ==================== 打回操作测试 ====================

    @Test
    @DisplayName("应该成功打回申请")
    void shouldReturnTaskSuccessfully() {
        // Given
        Long instanceId = 1L;
        String taskId = "task-123";
        String targetNodeKey = "start";
        String userId = "user-1";
        String comment = "需要补充材料";
        doNothing().when(workflowService).returnTo(instanceId, taskId, targetNodeKey, comment);

        // When
        workflowIntegrationService.returnTask(instanceId, taskId, targetNodeKey, userId, comment);

        // Then
        verify(workflowService).returnTo(instanceId, taskId, targetNodeKey, comment);
    }

    @Test
    @DisplayName("打回申请时意见为空应该抛出异常")
    void shouldThrowExceptionWhenReturnWithEmptyComment() {
        // Given
        Long instanceId = 1L;
        String taskId = "task-123";
        String targetNodeKey = "start";
        String userId = "user-1";
        String comment = "   ";

        // When & Then
        assertThatThrownBy(() -> workflowIntegrationService.returnTask(instanceId, taskId, targetNodeKey, userId, comment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("打回申请必须填写意见");
        
        verify(workflowService, never()).returnTo(any(), any(), any(), any());
    }

    @Test
    @DisplayName("打回申请失败时应该抛出异常")
    void shouldThrowExceptionWhenReturnFails() {
        // Given
        Long instanceId = 1L;
        String taskId = "task-123";
        String targetNodeKey = "start";
        String userId = "user-1";
        String comment = "需要补充材料";
        doThrow(new RuntimeException("节点不存在")).when(workflowService).returnTo(instanceId, taskId, targetNodeKey, comment);

        // When & Then
        assertThatThrownBy(() -> workflowIntegrationService.returnTask(instanceId, taskId, targetNodeKey, userId, comment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("打回申请失败");
        
        verify(workflowService).returnTo(instanceId, taskId, targetNodeKey, comment);
    }

    // ==================== 批量审批测试 ====================

    @Test
    @DisplayName("应该成功执行批量审批")
    void shouldBatchApproveSuccessfully() {
        // Given
        List<ApprovalRequest> requests = createTestApprovalRequests();
        BatchResult expectedResult = createTestBatchResult();
        when(workflowService.batchApprove(requests)).thenReturn(expectedResult);

        // When
        BatchResult result = workflowIntegrationService.batchApprove(requests);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSuccessCount()).isEqualTo(expectedResult.getSuccessCount());
        assertThat(result.getFailureCount()).isEqualTo(expectedResult.getFailureCount());
        verify(workflowService).batchApprove(requests);
    }

    @Test
    @DisplayName("批量审批失败时应该抛出异常")
    void shouldThrowExceptionWhenBatchApproveFails() {
        // Given
        List<ApprovalRequest> requests = createTestApprovalRequests();
        when(workflowService.batchApprove(requests)).thenThrow(new RuntimeException("批量操作失败"));

        // When & Then
        assertThatThrownBy(() -> workflowIntegrationService.batchApprove(requests))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("批量审批失败");
        
        verify(workflowService).batchApprove(requests);
    }

    // ==================== 获取可打回节点测试 ====================

    @Test
    @DisplayName("应该成功获取可打回的节点列表")
    void shouldGetReturnableNodesSuccessfully() {
        // Given
        Long nodeId = 1L;
        List<Map<String, String>> expectedNodes = createTestReturnableNodes();
        when(workflowService.getReturnableNodes(nodeId)).thenReturn(expectedNodes);

        // When
        List<Map<String, String>> result = workflowIntegrationService.getReturnableNodes(nodeId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsEntry("key", "start");
        assertThat(result.get(1)).containsEntry("key", "manager_review");
        verify(workflowService).getReturnableNodes(nodeId);
    }

    @Test
    @DisplayName("获取可打回节点失败时应该抛出异常")
    void shouldThrowExceptionWhenGetReturnableNodesFails() {
        // Given
        Long nodeId = 1L;
        when(workflowService.getReturnableNodes(nodeId)).thenThrow(new RuntimeException("节点查询失败"));

        // When & Then
        assertThatThrownBy(() -> workflowIntegrationService.getReturnableNodes(nodeId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("获取可打回节点失败");
        
        verify(workflowService).getReturnableNodes(nodeId);
    }

    // ==================== 获取工作流详情测试 ====================

    @Test
    @DisplayName("应该成功获取工作流实例详情")
    void shouldGetWorkflowInstanceDetailSuccessfully() {
        // Given
        String businessType = "EXPENSE";
        String businessId = "EXP202412001";

        // When
        WorkflowInstanceDetailDTO result = workflowIntegrationService.getWorkflowInstanceDetail(businessType, businessId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getInstanceId()).isEqualTo(1L);
        assertThat(result.getTitle()).contains(businessId);
        assertThat(result.getStatus()).isEqualTo("RUNNING");
        assertThat(result.getSteps()).hasSize(3);
        assertThat(result.getProgress()).isGreaterThan(0);
        
        // 验证步骤详情
        assertThat(result.getSteps().get(0).getStatus()).isEqualTo("completed");
        assertThat(result.getSteps().get(1).getStatus()).isEqualTo("active");
        assertThat(result.getSteps().get(2).getStatus()).isEqualTo("pending");
    }

    @Test
    @DisplayName("应该成功获取工作流历史记录")
    void shouldGetWorkflowHistorySuccessfully() {
        // Given
        String businessType = "EXPENSE";
        String businessId = "EXP202412001";

        // When
        List<WorkflowHistoryDTO> result = workflowIntegrationService.getWorkflowHistory(businessType, businessId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTaskName()).isEqualTo("直属主管审批");
        assertThat(result.get(0).getOperationType()).isEqualTo("COMPLETE");
        assertThat(result.get(0).getOperatorName()).isEqualTo("张三");
        assertThat(result.get(0).getComment()).isEqualTo("同意申请");
    }

    // ==================== 测试数据创建方法 ====================

    /**
     * 创建测试费用申请实体
     */
    private ExpenseApplication createTestExpenseApplication() {
        ExpenseApplication application = new ExpenseApplication();
        application.setId(1L);
        application.setApplicationNumber("EXP202412001");
        application.setApplicantId(1L);
        application.setDescription("差旅费报销");
        application.setTotalAmount(new BigDecimal("1500.00"));
        application.setCurrency("CNY");
        application.setCompany("测试公司");
        application.setApplyDate(LocalDate.now());
        return application;
    }

    /**
     * 创建测试工作流实例
     */
    private WorkflowInstanceDTO createTestWorkflowInstance() {
        WorkflowInstanceDTO instance = new WorkflowInstanceDTO();
        instance.setProcessInstanceId("process-123");
        instance.setBusinessType("EXPENSE");
        instance.setBusinessId("EXP202412001");
        instance.setTitle("费用申请审批 - EXP202412001");
        instance.setStatus("RUNNING");
        instance.setStartTime(LocalDateTime.now());
        return instance;
    }

    /**
     * 创建测试待办任务
     */
    private PendingTaskDTO createTestPendingTask() {
        PendingTaskDTO task = new PendingTaskDTO();
        task.setTaskId("task-123");
        task.setTaskName("财务审批");
        task.setProcessInstanceId("process-123");
        task.setBusinessType("EXPENSE");
        task.setBusinessId("EXP202412001");
        task.setTitle("费用申请审批");
        task.setAssignee("财务经理");
        task.setCreateTime("2025-01-12T20:00:00"); // 设置为String类型
        task.setPriority("medium"); // 设置为String类型
        return task;
    }

    /**
     * 创建测试审批请求列表
     */
    private List<ApprovalRequest> createTestApprovalRequests() {
        List<ApprovalRequest> requests = new ArrayList<>();
        
        ApprovalRequest request1 = new ApprovalRequest();
        request1.setTaskId(1L); // ApprovalRequest的taskId是Long类型
        request1.setComment("批量审批通过");
        requests.add(request1);
        
        ApprovalRequest request2 = new ApprovalRequest();
        request2.setTaskId(2L); // ApprovalRequest的taskId是Long类型
        request2.setComment("批量审批通过");
        requests.add(request2);
        
        return requests;
    }

    /**
     * 创建测试批量结果
     */
    private BatchResult createTestBatchResult() {
        BatchResult result = new BatchResult();
        result.setSuccessCount(2);
        result.setFailureCount(0);
        result.setSuccessIds(Arrays.asList(1L, 2L)); // 使用setSuccessIds方法，传入Long类型ID列表
        result.setFailureIds(new ArrayList<>()); // 使用setFailureIds方法
        return result;
    }

    /**
     * 创建测试可打回节点列表
     */
    private List<Map<String, String>> createTestReturnableNodes() {
        List<Map<String, String>> nodes = new ArrayList<>();
        
        Map<String, String> node1 = new HashMap<>();
        node1.put("key", "start");
        node1.put("name", "开始节点");
        nodes.add(node1);
        
        Map<String, String> node2 = new HashMap<>();
        node2.put("key", "manager_review");
        node2.put("name", "主管审核");
        nodes.add(node2);
        
        return nodes;
    }
} 