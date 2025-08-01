package demo.backed.UT.WorkflowService;

import demo.backed.BaseServiceTest;
import demo.backed.dto.*;
import demo.backed.entity.WorkflowInstance;
import demo.backed.entity.WorkflowNode;
import demo.backed.repository.WorkflowInstanceRepository;
import demo.backed.repository.WorkflowNodeRepository;
import demo.backed.service.UserService;
import demo.backed.service.WorkflowService;
import demo.backed.service.WorkflowTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WorkflowService单元测试
 */
@DisplayName("工作流服务测试")
class WorkflowServiceTest extends BaseServiceTest {

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private TaskService taskService;

    @Mock
    private HistoryService historyService;

    @Mock
    private UserService userService;

    @Mock
    private WorkflowInstanceRepository workflowInstanceRepository;

    @Mock
    private WorkflowNodeRepository workflowNodeRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WorkflowTemplateService workflowTemplateService;

    @Mock
    private RepositoryService repositoryService;

    @InjectMocks
    private WorkflowService workflowService;

    private StartProcessRequest testStartRequest;
    private WorkflowInstance testInstance;
    private UserDTO testUser;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testStartRequest = createTestStartProcessRequest();
        testInstance = createTestWorkflowInstance();
        testUser = createTestUser();
    }

    // ==================== 启动工作流测试 ====================

    @Test
    @DisplayName("启动工作流时申请人不存在应该抛出异常")
    void shouldThrowExceptionWhenApplicantNotExists() {
        // Given
        when(workflowInstanceRepository.save(any(WorkflowInstance.class))).thenReturn(testInstance);
        when(userService.getUserById(testStartRequest.getApplicantId())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> workflowService.startWorkflow(testStartRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("申请人不存在");
        
        verify(workflowInstanceRepository).delete(any(WorkflowInstance.class));
    }

    @Test
    @DisplayName("启动工作流时没有可用模板应该抛出异常")
    void shouldThrowExceptionWhenNoTemplateAvailable() {
        // Given
        when(workflowInstanceRepository.save(any(WorkflowInstance.class))).thenReturn(testInstance);
        when(userService.getUserById(testStartRequest.getApplicantId())).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> workflowService.startWorkflow(testStartRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("无法找到合适的审批人");
        
        verify(workflowInstanceRepository).delete(any(WorkflowInstance.class));
    }

    // ==================== 获取待办任务测试 ====================

    @Test
    @DisplayName("获取待办任务时用户ID为空应该返回空列表")
    void shouldReturnEmptyListWhenUserIdIsNull() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<PendingTaskDTO> result = workflowService.getPendingTasks(null, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("获取待办任务时发生异常应该返回空列表")
    void shouldReturnEmptyListWhenExceptionOccurs() {
        // Given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        when(taskService.createTaskQuery()).thenThrow(new RuntimeException("数据库连接错误"));

        // When
        Page<PendingTaskDTO> result = workflowService.getPendingTasks(userId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    // ==================== 获取已办任务测试 ====================

    @Test
    @DisplayName("获取已办任务时用户ID为空应该返回空列表")
    void shouldReturnEmptyListWhenUserIdIsNullForHandledTasks() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<PendingTaskDTO> result = workflowService.getHandledTasks(null, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    // ==================== 审批操作测试 ====================

    @Test
    @DisplayName("审批通过时任务不存在应该抛出异常")
    void shouldThrowExceptionWhenTaskNotExistsForApprove() {
        // Given
        Long instanceId = 1L;
        String taskId = "task-123";
        String comment = "审批通过";
        
        TaskQuery taskQuery = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskId(taskId)).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> workflowService.approve(instanceId, taskId, comment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("任务不存在");
    }

    @Test
    @DisplayName("审批拒绝时任务不存在应该抛出异常")
    void shouldThrowExceptionWhenTaskNotExistsForReject() {
        // Given
        Long instanceId = 1L;
        String taskId = "task-123";
        String comment = "费用不合理";
        
        TaskQuery taskQuery = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskId(taskId)).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> workflowService.reject(instanceId, taskId, comment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("任务不存在");
    }

    // ==================== 打回操作测试 ====================

    @Test
    @DisplayName("打回申请时任务不存在应该抛出异常")
    void shouldThrowExceptionWhenTaskNotExistsForReturn() {
        // Given
        Long instanceId = 1L;
        String taskId = "task-123";
        String targetNodeKey = "start";
        String comment = "需要补充材料";
        
        TaskQuery taskQuery = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskId(taskId)).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> workflowService.returnTo(instanceId, taskId, targetNodeKey, comment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("任务不存在");
    }

    // ==================== 获取流程实例测试 ====================

    @Test
    @DisplayName("获取流程实例时ID不存在应该抛出异常")
    void shouldThrowExceptionWhenInstanceNotExists() {
        // Given
        Long instanceId = 1L;
        when(workflowInstanceRepository.findById(instanceId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> workflowService.getWorkflowInstance(instanceId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("流程实例不存在");
    }

    // ==================== 批量审批测试 ====================

    @Test
    @DisplayName("批量审批时部分失败应该记录错误")
    void shouldRecordErrorsWhenBatchApprovePartiallyFails() {
        // Given
        List<ApprovalRequest> requests = createTestApprovalRequests();
        
        // 第一个成功，第二个失败
        when(workflowNodeRepository.findById(1L)).thenReturn(Optional.of(createTestWorkflowNode()));
        when(workflowNodeRepository.findById(2L)).thenReturn(Optional.empty());

        // When
        BatchResult result = workflowService.batchApprove(requests);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSuccessCount()).isEqualTo(0); // 第一个也会因为后续步骤失败
        assertThat(result.getFailureCount()).isEqualTo(2);
        assertThat(result.getErrors()).hasSize(2);
    }

    // ==================== 获取可打回节点测试 ====================

    @Test
    @DisplayName("获取可打回节点时节点不存在应该抛出异常")
    void shouldThrowExceptionWhenNodeNotExistsForReturnableNodes() {
        // Given
        Long nodeId = 1L;
        when(workflowNodeRepository.findById(nodeId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> workflowService.getReturnableNodes(nodeId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("节点不存在");
    }

    // ==================== 测试数据创建方法 ====================

    /**
     * 创建测试启动流程请求
     */
    private StartProcessRequest createTestStartProcessRequest() {
        StartProcessRequest request = new StartProcessRequest();
        request.setBusinessType("EXPENSE");
        request.setBusinessId("EXP202412001");
        request.setApplicantId(1L);
        request.setTitle("费用申请审批");
        request.setAmount(new BigDecimal("1500.00"));
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("description", "差旅费报销");
        request.setVariables(variables);
        
        return request;
    }

    /**
     * 创建测试工作流实例
     */
    private WorkflowInstance createTestWorkflowInstance() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(1L);
        instance.setProcessInstanceId("process-123");
        instance.setBusinessType("EXPENSE");
        instance.setBusinessId("EXP202412001");
        instance.setTitle("费用申请审批");
        instance.setApplicantId(1L);
        instance.setApplicantName("张三");
        instance.setStatus("RUNNING");
        instance.setStartTime(LocalDateTime.now());
        instance.setCurrentNodeName("财务审批");
        instance.setCurrentAssignee("2");
        return instance;
    }

    /**
     * 创建测试用户
     */
    private UserDTO createTestUser() {
        UserDTO user = new UserDTO();
        user.setId(1L);
        user.setUserName("张三");
        user.setEmail("zhangsan@hkex.com");
        user.setDepartment("技术部");
        user.setPosition("开发工程师");
        user.setStatus("在职");
        user.setManagerId(2L);
        return user;
    }

    /**
     * 创建测试工作流节点
     */
    private WorkflowNode createTestWorkflowNode() {
        WorkflowNode node = new WorkflowNode();
        node.setId(1L);
        node.setInstanceId(1L);
        node.setTaskId("task-123");
        node.setNodeKey("financeApproval");
        node.setNodeName("财务审批");
        node.setStatus("PENDING");
        node.setAssigneeId(2L);
        node.setAssigneeName("李四");
        node.setExecutionId("execution-123");
        return node;
    }

    /**
     * 创建测试审批请求列表
     */
    private List<ApprovalRequest> createTestApprovalRequests() {
        List<ApprovalRequest> requests = new ArrayList<>();
        
        ApprovalRequest request1 = new ApprovalRequest();
        request1.setTaskId(1L);
        request1.setAction("approve");
        request1.setComment("批量审批通过");
        requests.add(request1);
        
        ApprovalRequest request2 = new ApprovalRequest();
        request2.setTaskId(2L);
        request2.setAction("approve");
        request2.setComment("批量审批通过");
        requests.add(request2);
        
        return requests;
    }
} 