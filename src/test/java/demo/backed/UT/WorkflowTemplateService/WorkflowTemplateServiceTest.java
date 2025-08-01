package demo.backed.UT.WorkflowTemplateService;

import demo.backed.BaseServiceTest;
import demo.backed.entity.WorkflowTemplate;
import demo.backed.repository.WorkflowTemplateRepository;
import demo.backed.service.WorkflowTemplateService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WorkflowTemplateService单元测试
 */
@DisplayName("工作流模板服务测试")
class WorkflowTemplateServiceTest extends BaseServiceTest {

    @Mock
    private WorkflowTemplateRepository templateRepository;

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private DeploymentBuilder deploymentBuilder;

    @Mock
    private Deployment deployment;

    @Mock
    private ProcessDefinitionQuery processDefinitionQuery;

    @InjectMocks
    private WorkflowTemplateService workflowTemplateService;

    private WorkflowTemplate testTemplate;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testTemplate = createTestWorkflowTemplate();
    }

    // ==================== 基础CRUD操作测试 ====================

    @Test
    @DisplayName("应该成功获取所有模板")
    void shouldGetAllTemplatesSuccessfully() {
        // Given
        List<WorkflowTemplate> templates = Arrays.asList(testTemplate);
        when(templateRepository.findAllActive()).thenReturn(templates);

        // When
        List<WorkflowTemplate> result = workflowTemplateService.getAllTemplates();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo(testTemplate.getName());
        verify(templateRepository).findAllActive();
    }

    @Test
    @DisplayName("应该根据ID成功获取模板")
    void shouldGetTemplateByIdSuccessfully() {
        // Given
        Long templateId = createTestTemplateId();
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));

        // When
        WorkflowTemplate result = workflowTemplateService.getTemplateById(templateId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(testTemplate.getName());
        verify(templateRepository).findById(templateId);
    }

    @Test
    @DisplayName("获取不存在的模板应该抛出异常")
    void shouldThrowExceptionWhenTemplateNotFound() {
        // Given
        Long templateId = createTestTemplateId();
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> workflowTemplateService.getTemplateById(templateId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("工作流模板不存在");
        
        verify(templateRepository).findById(templateId);
    }

    @Test
    @DisplayName("应该成功创建工作流模板")
    void shouldCreateTemplateSuccessfully() {
        // Given
        WorkflowTemplate newTemplate = createTestWorkflowTemplate();
        newTemplate.setId(null);
        newTemplate.setProcessKey(null); // 让系统自动生成
        
        when(templateRepository.existsByProcessKey(anyString())).thenReturn(false);
        when(templateRepository.save(any(WorkflowTemplate.class))).thenReturn(testTemplate);

        // When
        WorkflowTemplate result = workflowTemplateService.createTemplate(newTemplate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(testTemplate.getName());
        verify(templateRepository).existsByProcessKey(anyString());
        verify(templateRepository).save(any(WorkflowTemplate.class));
    }

    @Test
    @DisplayName("创建重复ProcessKey的模板应该抛出异常")
    void shouldThrowExceptionWhenProcessKeyDuplicate() {
        // Given
        WorkflowTemplate newTemplate = createTestWorkflowTemplate();
        newTemplate.setProcessKey("existing_key");
        
        when(templateRepository.existsByProcessKey("existing_key")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> workflowTemplateService.createTemplate(newTemplate))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("流程Key已存在");
        
        verify(templateRepository, never()).save(any(WorkflowTemplate.class));
    }

    @Test
    @DisplayName("应该成功更新工作流模板")
    void shouldUpdateTemplateSuccessfully() {
        // Given
        Long templateId = createTestTemplateId();
        WorkflowTemplate updateData = new WorkflowTemplate();
        updateData.setName("更新后的模板名称");
        updateData.setDescription("更新后的描述");
        updateData.setType("expense");
        updateData.setConfigData("{\"updated\": true}");
        
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));
        when(templateRepository.save(any(WorkflowTemplate.class))).thenReturn(testTemplate);

        // When
        WorkflowTemplate result = workflowTemplateService.updateTemplate(templateId, updateData);

        // Then
        assertThat(result).isNotNull();
        verify(templateRepository).findById(templateId);
        verify(templateRepository).save(any(WorkflowTemplate.class));
    }

    @Test
    @DisplayName("应该成功删除工作流模板")
    void shouldDeleteTemplateSuccessfully() {
        // Given
        Long templateId = createTestTemplateId();
        testTemplate.setIsDeployed(false);
        testTemplate.setDeploymentId(null);
        
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));
        when(templateRepository.save(any(WorkflowTemplate.class))).thenReturn(testTemplate);

        // When
        workflowTemplateService.deleteTemplate(templateId);

        // Then
        verify(templateRepository).findById(templateId);
        verify(templateRepository).save(any(WorkflowTemplate.class));
    }

    @Test
    @DisplayName("删除已部署的模板应该先取消部署")
    void shouldUndeployBeforeDeleteWhenTemplateIsDeployed() {
        // Given
        Long templateId = createTestTemplateId();
        testTemplate.setIsDeployed(true);
        testTemplate.setDeploymentId("deployment-123");
        
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));
        when(templateRepository.save(any(WorkflowTemplate.class))).thenReturn(testTemplate);
        doNothing().when(repositoryService).deleteDeployment("deployment-123", true);

        // When
        workflowTemplateService.deleteTemplate(templateId);

        // Then
        verify(templateRepository).findById(templateId);
        verify(repositoryService).deleteDeployment("deployment-123", true);
        verify(templateRepository).save(any(WorkflowTemplate.class));
    }

    // ==================== 部署相关测试 ====================

    @Test
    @DisplayName("应该成功部署工作流模板")
    void shouldDeployTemplateSuccessfully() {
        // Given
        Long templateId = createTestTemplateId();
        testTemplate.setBpmnXml("<?xml version=\"1.0\"?><definitions>...</definitions>");
        
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));
        when(repositoryService.createDeployment()).thenReturn(deploymentBuilder);
        when(deploymentBuilder.name(anyString())).thenReturn(deploymentBuilder);
        when(deploymentBuilder.category(anyString())).thenReturn(deploymentBuilder);
        when(deploymentBuilder.key(anyString())).thenReturn(deploymentBuilder);
        when(deploymentBuilder.addBytes(anyString(), any(byte[].class))).thenReturn(deploymentBuilder);
        when(deploymentBuilder.deploy()).thenReturn(deployment);
        when(deployment.getId()).thenReturn("deployment-123");
        when(templateRepository.save(any(WorkflowTemplate.class))).thenReturn(testTemplate);

        // When
        WorkflowTemplate result = workflowTemplateService.deployTemplate(templateId);

        // Then
        assertThat(result).isNotNull();
        verify(templateRepository).findById(templateId);
        verify(repositoryService).createDeployment();
        verify(deploymentBuilder).deploy();
        verify(templateRepository).save(any(WorkflowTemplate.class));
    }

    @Test
    @DisplayName("部署空BPMN内容的模板应该抛出异常")
    void shouldThrowExceptionWhenDeployEmptyBpmn() {
        // Given
        Long templateId = createTestTemplateId();
        testTemplate.setBpmnXml(null);
        
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));

        // When & Then
        assertThatThrownBy(() -> workflowTemplateService.deployTemplate(templateId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("BPMN XML内容为空，无法部署");
        
        verify(repositoryService, never()).createDeployment();
    }

    @Test
    @DisplayName("应该成功取消部署工作流模板")
    void shouldUndeployTemplateSuccessfully() {
        // Given
        Long templateId = createTestTemplateId();
        testTemplate.setIsDeployed(true);
        testTemplate.setDeploymentId("deployment-123");
        
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));
        doNothing().when(repositoryService).deleteDeployment("deployment-123", true);
        when(templateRepository.save(any(WorkflowTemplate.class))).thenReturn(testTemplate);

        // When
        WorkflowTemplate result = workflowTemplateService.undeployTemplate(templateId);

        // Then
        assertThat(result).isNotNull();
        verify(templateRepository).findById(templateId);
        verify(repositoryService).deleteDeployment("deployment-123", true);
        verify(templateRepository).save(any(WorkflowTemplate.class));
    }

    @Test
    @DisplayName("取消部署未部署的模板应该抛出异常")
    void shouldThrowExceptionWhenUndeployNonDeployedTemplate() {
        // Given
        Long templateId = createTestTemplateId();
        testTemplate.setIsDeployed(false);
        testTemplate.setDeploymentId(null);
        
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(testTemplate));

        // When & Then
        assertThatThrownBy(() -> workflowTemplateService.undeployTemplate(templateId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("工作流模板未部署");
        
        verify(repositoryService, never()).deleteDeployment(anyString(), anyBoolean());
    }

    // ==================== 业务查找相关测试 ====================

    @Test
    @DisplayName("应该根据业务类型找到已部署的模板")
    void shouldFindDeployedTemplateByBusinessType() {
        // Given
        String businessType = "expense";
        testTemplate.setType("expense");
        testTemplate.setIsDeployed(true);
        testTemplate.setStatus("active");
        
        List<WorkflowTemplate> deployedTemplates = Arrays.asList(testTemplate);
        when(templateRepository.findByIsDeployedAndStatus(true, "active")).thenReturn(deployedTemplates);

        // When
        Optional<WorkflowTemplate> result = workflowTemplateService.findDeployedTemplateByBusinessType(businessType);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getType()).isEqualTo("expense");
        verify(templateRepository).findByIsDeployedAndStatus(true, "active");
    }

    @Test
    @DisplayName("业务类型不匹配时应该返回第一个可用模板作为fallback")
    void shouldReturnFallbackTemplateWhenBusinessTypeNotMatch() {
        // Given
        String businessType = "unknown_type";
        testTemplate.setType("expense");
        testTemplate.setIsDeployed(true);
        testTemplate.setStatus("active");
        
        List<WorkflowTemplate> deployedTemplates = Arrays.asList(testTemplate);
        when(templateRepository.findByIsDeployedAndStatus(true, "active")).thenReturn(deployedTemplates);

        // When
        Optional<WorkflowTemplate> result = workflowTemplateService.findDeployedTemplateByBusinessType(businessType);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getType()).isEqualTo("expense");
        verify(templateRepository).findByIsDeployedAndStatus(true, "active");
    }

    @Test
    @DisplayName("没有已部署模板时应该返回空")
    void shouldReturnEmptyWhenNoDeployedTemplates() {
        // Given
        String businessType = "expense";
        List<WorkflowTemplate> emptyList = Arrays.asList();
        when(templateRepository.findByIsDeployedAndStatus(true, "active")).thenReturn(emptyList);

        // When
        Optional<WorkflowTemplate> result = workflowTemplateService.findDeployedTemplateByBusinessType(businessType);

        // Then
        assertThat(result).isEmpty();
        verify(templateRepository).findByIsDeployedAndStatus(true, "active");
    }

    @Test
    @DisplayName("应该获取默认的费用审批模板")
    void shouldGetDefaultExpenseTemplate() {
        // Given
        testTemplate.setType("expense");
        testTemplate.setIsDeployed(true);
        testTemplate.setStatus("active");
        
        List<WorkflowTemplate> templates = Arrays.asList(testTemplate);
        when(templateRepository.findByIsDeployedAndStatus(true, "active")).thenReturn(templates);

        // When
        Optional<WorkflowTemplate> result = workflowTemplateService.getDefaultExpenseTemplate();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getType()).isEqualTo("expense");
        verify(templateRepository).findByIsDeployedAndStatus(true, "active");
    }

    // ==================== 模板可用性验证测试 ====================

    @Test
    @DisplayName("应该验证模板可用性")
    void shouldValidateTemplateAvailability() {
        // Given
        String processKey = "expense_approval";
        
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.processDefinitionKey(processKey)).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.active()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.count()).thenReturn(1L);

        // When
        boolean result = workflowTemplateService.isTemplateAvailable(processKey);

        // Then
        assertThat(result).isTrue();
        verify(repositoryService).createProcessDefinitionQuery();
        verify(processDefinitionQuery).processDefinitionKey(processKey);
    }

    @Test
    @DisplayName("模板不可用时应该返回false")
    void shouldReturnFalseWhenTemplateNotAvailable() {
        // Given
        String processKey = "non_existent_process";
        
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.processDefinitionKey(anyString())).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.active()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.count()).thenReturn(0L);

        // When
        boolean result = workflowTemplateService.isTemplateAvailable(processKey);

        // Then
        assertThat(result).isFalse();
        verify(repositoryService, atLeastOnce()).createProcessDefinitionQuery();
    }

    @Test
    @DisplayName("模板可用性验证异常时应该返回false")
    void shouldReturnFalseWhenValidationThrowsException() {
        // Given
        String processKey = "test_process";
        
        when(repositoryService.createProcessDefinitionQuery()).thenThrow(new RuntimeException("Flowable error"));

        // When
        boolean result = workflowTemplateService.isTemplateAvailable(processKey);

        // Then
        assertThat(result).isFalse();
        verify(repositoryService).createProcessDefinitionQuery();
    }

    // ==================== 测试数据创建方法 ====================

    /**
     * 创建测试工作流模板
     */
    private WorkflowTemplate createTestWorkflowTemplate() {
        WorkflowTemplate template = new WorkflowTemplate();
        template.setId(createTestTemplateId());
        template.setName("费用审批流程");
        template.setDescription("用于费用申请的审批流程模板");
        template.setProcessKey("expense_approval_test");
        template.setType("expense");
        template.setStatus("draft");
        template.setBpmnXml("<?xml version=\"1.0\" encoding=\"UTF-8\"?><definitions>...</definitions>");
        template.setConfigData("{\"steps\": []}");
        template.setTemplateVersion(1);
        template.setIsDeployed(false);
        template.setDeploymentId(null);
        template.setDeployedTime(null);
        template.setCreatedTime(LocalDateTime.now());
        template.setUpdatedTime(LocalDateTime.now());
        return template;
    }

    /**
     * 创建测试模板ID
     */
    protected Long createTestTemplateId() {
        return 1L;
    }
} 