package demo.backed.controller;

import demo.backed.dto.*;
import demo.backed.service.WorkflowIntegrationService;
import demo.backed.service.ExpenseApplicationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作流追踪控制器
 * 提供动态工作流状态追踪API
 */
@RestController
@RequestMapping("/api/workflow-tracker")
@Api(tags = "工作流追踪")
@Slf4j
public class WorkflowTrackerController {
    
    @Autowired
    private WorkflowIntegrationService workflowIntegrationService;
    
    @Autowired
    private ExpenseApplicationService expenseApplicationService;
    
    /**
     * 根据费用申请ID获取工作流追踪信息
     * 支持动态工作流模板的状态追踪
     */
    @GetMapping("/expense/{applicationId}")
    @ApiOperation("获取费用申请工作流追踪")
    public ApiResponse<WorkflowTrackerDTO> getExpenseWorkflowTracker(@PathVariable Long applicationId) {
        try {
            // 1. 获取费用申请信息
            ExpenseApplicationDTO application = expenseApplicationService.getApplicationDetail(applicationId);
            
            if (application.getWorkflowInstanceId() == null) {
                return ApiResponse.error("该申请尚未启动工作流");
            }
            
            // 2. 获取工作流追踪信息（支持动态工作流模板）
            WorkflowInstanceDetailDTO detail = workflowIntegrationService
                .getWorkflowInstanceDetail("EXPENSE", applicationId.toString());
            
            // 3. 构建前端所需的工作流追踪数据
            WorkflowTrackerDTO tracker = new WorkflowTrackerDTO();
            tracker.setId(detail.getInstanceId().toString());
            tracker.setTitle(detail.getTitle());
            tracker.setApplicant(detail.getApplicantName());
            tracker.setApplicationDate(detail.getStartTime());
            tracker.setStatus(mapWorkflowStatus(detail.getStatus()));
            tracker.setCurrentStep(getCurrentStepIndex(detail.getSteps()));
            
            // 4. 构建步骤信息（动态工作流模板的步骤）
            List<WorkflowStepDTO> steps = detail.getSteps().stream()
                .map(this::convertToWorkflowStep)
                .collect(Collectors.toList());
            tracker.setSteps(steps);
            
            // 5. 计算进度
            tracker.setProgress(detail.getProgress());
            tracker.setBusinessType("EXPENSE");
            tracker.setBusinessId(applicationId.toString());
            
            log.info("获取动态工作流追踪成功，费用申请ID: {}, 工作流模板: {}", 
                    applicationId, detail.getWorkflowTemplateName());
            return ApiResponse.success(tracker);
        } catch (Exception e) {
            log.error("获取工作流追踪失败，费用申请ID: {}", applicationId, e);
            return ApiResponse.error("获取工作流追踪失败: " + e.getMessage());
        }
    }
    
    /**
     * 通用工作流追踪接口
     * 支持不同业务类型（费用申请、差旅报销等）
     */
    @GetMapping("/{businessType}/{businessId}")
    @ApiOperation("获取通用工作流追踪")
    public ApiResponse<WorkflowTrackerDTO> getWorkflowTracker(
            @PathVariable String businessType,
            @PathVariable String businessId) {
        try {
            log.info("获取工作流追踪，业务类型: {}, 业务ID: {}", businessType, businessId);
            
            // 获取工作流实例详情
            WorkflowInstanceDetailDTO detail = workflowIntegrationService
                .getWorkflowInstanceDetail(businessType.toUpperCase(), businessId);
            
            if (detail == null) {
                return ApiResponse.error("未找到对应的工作流实例");
            }
            
            // 构建追踪数据
            WorkflowTrackerDTO tracker = buildWorkflowTracker(detail, businessType, businessId);
            
            log.info("获取工作流追踪成功，业务类型: {}, 实例ID: {}", businessType, detail.getInstanceId());
            return ApiResponse.success(tracker);
        } catch (Exception e) {
            log.error("获取工作流追踪失败，业务类型: {}, 业务ID: {}", businessType, businessId, e);
            return ApiResponse.error("获取工作流追踪失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取工作流历史记录
     */
    @GetMapping("/{businessType}/{businessId}/history")
    @ApiOperation("获取工作流历史记录")
    public ApiResponse<List<WorkflowHistoryDTO>> getWorkflowHistory(
            @PathVariable String businessType,
            @PathVariable String businessId) {
        try {
            List<WorkflowHistoryDTO> history = workflowIntegrationService
                .getWorkflowHistory(businessType.toUpperCase(), businessId);
            
            log.info("获取工作流历史成功，业务类型: {}, 业务ID: {}, 记录数: {}", 
                    businessType, businessId, history.size());
            return ApiResponse.success(history);
        } catch (Exception e) {
            log.error("获取工作流历史失败，业务类型: {}, 业务ID: {}", businessType, businessId, e);
            return ApiResponse.error("获取工作流历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建工作流追踪数据
     */
    private WorkflowTrackerDTO buildWorkflowTracker(WorkflowInstanceDetailDTO detail, 
                                                  String businessType, String businessId) {
        WorkflowTrackerDTO tracker = new WorkflowTrackerDTO();
        tracker.setId(detail.getInstanceId().toString());
        tracker.setTitle(detail.getTitle());
        tracker.setApplicant(detail.getApplicantName());
        tracker.setApplicationDate(detail.getStartTime());
        tracker.setStatus(mapWorkflowStatus(detail.getStatus()));
        tracker.setCurrentStep(getCurrentStepIndex(detail.getSteps()));
        tracker.setBusinessType(businessType.toUpperCase());
        tracker.setBusinessId(businessId);
        
        // 构建步骤信息
        List<WorkflowStepDTO> steps = detail.getSteps().stream()
            .map(this::convertToWorkflowStep)
            .collect(Collectors.toList());
        tracker.setSteps(steps);
        
        // 计算进度
        tracker.setProgress(detail.getProgress());
        
        return tracker;
    }
    
    /**
     * 映射工作流状态
     */
    private String mapWorkflowStatus(String flowableStatus) {
        if (flowableStatus == null) {
            return "unknown";
        }
        
        switch (flowableStatus.toUpperCase()) {
            case "ACTIVE":
            case "RUNNING":
                return "in-progress";
            case "COMPLETED":
            case "FINISHED":
                return "completed";
            case "SUSPENDED":
                return "suspended";
            case "CANCELLED":
                return "cancelled";
            default:
                return "in-progress";
        }
    }
    
    /**
     * 获取当前步骤索引
     */
    private int getCurrentStepIndex(List<WorkflowInstanceDetailDTO.StepDetail> steps) {
        for (int i = 0; i < steps.size(); i++) {
            WorkflowInstanceDetailDTO.StepDetail step = steps.get(i);
            if ("active".equals(step.getStatus()) || "pending".equals(step.getStatus())) {
                return i;
            }
        }
        return steps.size(); // 如果没有活动步骤，说明已完成
    }
    
    /**
     * 转换步骤信息
     */
    private WorkflowStepDTO convertToWorkflowStep(WorkflowInstanceDetailDTO.StepDetail stepDetail) {
        WorkflowStepDTO step = new WorkflowStepDTO();
        step.setId(stepDetail.getTaskId());
        step.setName(stepDetail.getTaskName());
        step.setStatus(mapStepStatus(stepDetail.getStatus()));
        step.setApprover(stepDetail.getAssigneeName());
        step.setApprovedAt(stepDetail.getEndTime());
        step.setComment(stepDetail.getComment());
        step.setIcon(getStepIcon(stepDetail.getTaskName()));
        step.setDescription(stepDetail.getDescription());
        step.setAssigneeId(stepDetail.getAssigneeId());
        
        // 计算处理时长
        if (stepDetail.getStartTime() != null && stepDetail.getEndTime() != null) {
            long duration = java.time.Duration.between(stepDetail.getStartTime(), stepDetail.getEndTime()).toMinutes();
            step.setDuration((int) duration);
        }
        
        return step;
    }
    
    /**
     * 映射步骤状态
     */
    private String mapStepStatus(String status) {
        if (status == null) {
            return "pending";
        }
        
        switch (status.toLowerCase()) {
            case "completed":
            case "finished":
                return "completed";
            case "active":
            case "assigned":
                return "in-progress";
            case "pending":
                return "pending";
            case "rejected":
                return "rejected";
            case "returned":
                return "returned";
            default:
                return "pending";
        }
    }
    
    /**
     * 获取步骤图标
     */
    private String getStepIcon(String taskName) {
        if (taskName == null) {
            return "circle";
        }
        
        String taskNameLower = taskName.toLowerCase();
        if (taskNameLower.contains("开始") || taskNameLower.contains("start")) {
            return "play";
        } else if (taskNameLower.contains("主管") || taskNameLower.contains("manager")) {
            return "user";
        } else if (taskNameLower.contains("财务") || taskNameLower.contains("finance")) {
            return "dollar-sign";
        } else if (taskNameLower.contains("合规") || taskNameLower.contains("compliance")) {
            return "shield";
        } else if (taskNameLower.contains("高管") || taskNameLower.contains("executive")) {
            return "crown";
        } else if (taskNameLower.contains("结束") || taskNameLower.contains("end")) {
            return "check";
        } else {
            return "circle";
        }
    }
} 