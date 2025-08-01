package demo.backed.service;

import demo.backed.dto.*;

import demo.backed.entity.ExpenseApplication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流集成服务
 * 负责费用申请与Flowable工作流引擎的集成
 */
@Service
@Transactional
@Slf4j
public class WorkflowIntegrationService {
    
    @Autowired
    private WorkflowService workflowService;
    
    /**
     * 启动费用审批工作流
     */
    public String startExpenseApprovalWorkflow(ExpenseApplication application) {
        log.info("启动费用审批工作流，申请编号: {}, 申请人ID: {}", 
                application.getApplicationNumber(), application.getApplicantId());
        
        try {
            // 构建启动流程请求
            StartProcessRequest request = new StartProcessRequest();
            request.setBusinessType("EXPENSE");
            request.setBusinessId(application.getApplicationNumber());  // 🔧 使用申请编号作为业务ID，确保一致性
            request.setApplicantId(application.getApplicantId());
            request.setTitle("费用申请审批 - " + application.getApplicationNumber());
            request.setAmount(application.getTotalAmount());
            
            // 设置额外的流程变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("applicationId", application.getId());
            variables.put("applicationNumber", application.getApplicationNumber());
            variables.put("description", application.getDescription());
            variables.put("currency", application.getCurrency());
            variables.put("company", application.getCompany());
            variables.put("applyDate", application.getApplyDate());
            request.setVariables(variables);
            
            // 启动工作流
            WorkflowInstanceDTO workflowInstance = workflowService.startWorkflow(request);
            
            log.info("费用审批工作流启动成功，工作流实例ID: {}, 申请编号: {}", 
                    workflowInstance.getProcessInstanceId(), application.getApplicationNumber());
            
            return workflowInstance.getProcessInstanceId();
            
        } catch (Exception e) {
            log.error("启动费用审批工作流失败，申请编号: {}", application.getApplicationNumber(), e);
            throw new RuntimeException("启动工作流失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户待办任务列表
     */
    public Page<PendingTaskDTO> getPendingTasks(Long userId, Pageable pageable) {
        try {
            return workflowService.getPendingTasks(userId, pageable);
        } catch (Exception e) {
            log.error("获取用户待办任务失败，用户ID: {}", userId, e);
            throw new RuntimeException("获取待办任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户已办任务列表
     */
    public Page<PendingTaskDTO> getHandledTasks(Long userId, Pageable pageable) {
        try {
            return workflowService.getHandledTasks(userId, pageable);
        } catch (Exception e) {
            log.error("获取用户已办任务失败，用户ID: {}", userId, e);
            throw new RuntimeException("获取已办任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 审批通过
     */
    public void approveTask(Long instanceId, String taskId, String userId, String comment) {
        try {
            log.info("审批通过，实例ID: {}, 任务ID: {}, 审批人: {}", instanceId, taskId, userId);
            
            workflowService.approve(instanceId, taskId, comment);
            
            log.info("审批通过操作完成，任务ID: {}", taskId);
            
        } catch (Exception e) {
            log.error("审批通过失败，任务ID: {}", taskId, e);
            throw new RuntimeException("审批失败: " + e.getMessage());
        }
    }
    
    /**
     * 审批拒绝
     */
    public void rejectTask(Long instanceId, String taskId, String userId, String comment) {
        try {
            log.info("审批拒绝，实例ID: {}, 任务ID: {}, 审批人: {}", instanceId, taskId, userId);
            
            if (comment == null || comment.trim().isEmpty()) {
                throw new RuntimeException("拒绝审批必须填写意见");
            }
            
            workflowService.reject(instanceId, taskId, comment);
            
            log.info("审批拒绝操作完成，任务ID: {}", taskId);
            
        } catch (Exception e) {
            log.error("审批拒绝失败，任务ID: {}", taskId, e);
            throw new RuntimeException("拒绝审批失败: " + e.getMessage());
        }
    }
    
    /**
     * 打回申请
     */
    public void returnTask(Long instanceId, String taskId, String targetNodeKey, String userId, String comment) {
        try {
            log.info("打回申请，实例ID: {}, 任务ID: {}, 目标节点: {}, 操作人: {}", 
                    instanceId, taskId, targetNodeKey, userId);
            
            if (comment == null || comment.trim().isEmpty()) {
                throw new RuntimeException("打回申请必须填写意见");
            }
            
            workflowService.returnTo(instanceId, taskId, targetNodeKey, comment);
            
            log.info("打回申请操作完成，任务ID: {}", taskId);
            
        } catch (Exception e) {
            log.error("打回申请失败，任务ID: {}", taskId, e);
            throw new RuntimeException("打回申请失败: " + e.getMessage());
        }
    }
    

    

    

    

    

    
    /**
     * 批量审批
     */
    public BatchResult batchApprove(List<ApprovalRequest> requests) {
        try {
            log.info("执行批量审批，数量: {}", requests.size());
            
            BatchResult result = workflowService.batchApprove(requests);
            
            log.info("批量审批完成，成功: {}, 失败: {}", 
                    result.getSuccessCount(), result.getFailureCount());
            
            return result;
        } catch (Exception e) {
            log.error("批量审批失败", e);
            throw new RuntimeException("批量审批失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取可打回的节点列表
     */
    public List<Map<String, String>> getReturnableNodes(Long nodeId) {
        try {
            return workflowService.getReturnableNodes(nodeId);
        } catch (Exception e) {
            log.error("获取可打回节点列表失败，节点ID: {}", nodeId, e);
            throw new RuntimeException("获取可打回节点失败: " + e.getMessage());
        }
    }
    

    
    /**
     * 获取工作流实例详情（支持动态工作流模板）
     */
    public WorkflowInstanceDetailDTO getWorkflowInstanceDetail(String businessType, String businessId) {
        try {
            log.info("获取工作流实例详情，业务类型: {}, 业务ID: {}", businessType, businessId);
            
            // 构建模拟的工作流实例详情（后续可以通过具体的WorkflowService方法实现）
            WorkflowInstanceDetailDTO detail = new WorkflowInstanceDetailDTO();
            detail.setInstanceId(1L);
            detail.setTitle("费用申请审批 - " + businessId);
            detail.setApplicantName("申请人");
            detail.setStartTime(java.time.LocalDateTime.now().minusDays(1));
            detail.setStatus("RUNNING");
            detail.setWorkflowTemplateName("日常费用审批工作流");
            
            // 构建步骤详情
            List<WorkflowInstanceDetailDTO.StepDetail> steps = buildMockSteps();
            detail.setSteps(steps);
            
            // 计算进度
            int progress = calculateProgress(steps);
            detail.setProgress(progress);
            
            log.info("获取工作流实例详情成功（模拟数据）");
            return detail;
            
        } catch (Exception e) {
            log.error("获取工作流实例详情失败，业务类型: {}, 业务ID: {}", businessType, businessId, e);
            throw new RuntimeException("获取工作流实例详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取工作流历史记录
     */
    public List<WorkflowHistoryDTO> getWorkflowHistory(String businessType, String businessId) {
        try {
            log.info("获取工作流历史记录，业务类型: {}, 业务ID: {}", businessType, businessId);
            
            // 构建模拟的历史记录（后续可以通过具体的WorkflowService方法实现）
            List<WorkflowHistoryDTO> history = new ArrayList<>();
            
            WorkflowHistoryDTO historyItem = new WorkflowHistoryDTO();
            historyItem.setId("1");
            historyItem.setTaskName("直属主管审批");
            historyItem.setOperationType("COMPLETE");
            historyItem.setOperatorId(1L);
            historyItem.setOperatorName("张三");
            historyItem.setOperationTime(java.time.LocalDateTime.now().minusDays(1));
            historyItem.setDuration(30);
            historyItem.setComment("同意申请");
            
            history.add(historyItem);
            
            log.info("获取工作流历史记录成功（模拟数据），记录数: {}", history.size());
            return history;
            
        } catch (Exception e) {
            log.error("获取工作流历史记录失败，业务类型: {}, 业务ID: {}", businessType, businessId, e);
            throw new RuntimeException("获取工作流历史记录失败: " + e.getMessage());
        }
    }
    

    
    /**
     * 构建模拟的工作流步骤
     */
    private List<WorkflowInstanceDetailDTO.StepDetail> buildMockSteps() {
        List<WorkflowInstanceDetailDTO.StepDetail> steps = new ArrayList<>();
        
        // 第一步：直属主管审批（已完成）
        WorkflowInstanceDetailDTO.StepDetail step1 = new WorkflowInstanceDetailDTO.StepDetail();
        step1.setTaskId("task1");
        step1.setTaskName("直属主管审批");
        step1.setStatus("completed");
        step1.setAssigneeId(1L);
        step1.setAssigneeName("张三");
        step1.setStartTime(java.time.LocalDateTime.now().minusDays(1));
        step1.setEndTime(java.time.LocalDateTime.now().minusDays(1).plusHours(1));
        step1.setComment("同意申请");
        step1.setDescription("直属主管审核申请内容");
        steps.add(step1);
        
        // 第二步：财务部审批（进行中）
        WorkflowInstanceDetailDTO.StepDetail step2 = new WorkflowInstanceDetailDTO.StepDetail();
        step2.setTaskId("task2");
        step2.setTaskName("财务部审批");
        step2.setStatus("active");
        step2.setAssigneeId(2L);
        step2.setAssigneeName("李四");
        step2.setStartTime(java.time.LocalDateTime.now().minusDays(1).plusHours(1));
        step2.setDescription("财务部门审核费用合理性");
        steps.add(step2);
        
        // 第三步：合规部审批（待处理）
        WorkflowInstanceDetailDTO.StepDetail step3 = new WorkflowInstanceDetailDTO.StepDetail();
        step3.setTaskId("task3");
        step3.setTaskName("合规部审批");
        step3.setStatus("pending");
        step3.setAssigneeId(3L);
        step3.setAssigneeName("王五");
        step3.setDescription("合规部门审核合规性");
        steps.add(step3);
        
        return steps;
    }
    
    /**
     * 计算工作流进度
     */
    private int calculateProgress(List<WorkflowInstanceDetailDTO.StepDetail> steps) {
        if (steps == null || steps.isEmpty()) {
            return 0;
        }
        
        long completedSteps = steps.stream()
            .filter(step -> "completed".equals(step.getStatus()))
            .count();
        
        return (int) ((completedSteps * 100) / steps.size());
    }
} 