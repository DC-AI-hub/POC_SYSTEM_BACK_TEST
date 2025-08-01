package demo.backed.controller;

import demo.backed.dto.*;
import demo.backed.service.WorkflowIntegrationService;
import demo.backed.config.KeycloakJwtAuthenticationService;
import demo.backed.entity.User;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审批管理控制器
 * 替换前端硬编码数据源，提供真实的审批管理API
 */
@RestController
@RequestMapping("/api/approval")
@Api(tags = "审批管理")
@Slf4j
public class ApprovalController {
    
    @Autowired
    private WorkflowIntegrationService workflowIntegrationService;
    
    @Autowired(required = false)
    private KeycloakJwtAuthenticationService keycloakJwtAuthenticationService;
    

    
    /**
     * 测试审批API的认证和连通性
     */
    @GetMapping("/test")
    @ApiOperation("测试审批API认证")
    public ApiResponse<Map<String, Object>> testApprovalApi(
            Authentication authentication,
            HttpServletRequest request) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 认证信息
            result.put("authenticated", authentication != null && authentication.isAuthenticated());
            result.put("principal", authentication != null ? authentication.getPrincipal().getClass().getSimpleName() : "null");
            result.put("authorities", authentication != null ? authentication.getAuthorities().toString() : "null");
            
            // JWT过滤器属性
            result.put("userId", request.getAttribute("userId"));
            result.put("userName", request.getAttribute("userName"));
            result.put("userEmail", request.getAttribute("userEmail"));
            result.put("userType", request.getAttribute("userType"));
            result.put("department", request.getAttribute("department"));
            
            // 检查Authorization header
            String authHeader = request.getHeader("Authorization");
            result.put("hasAuthHeader", authHeader != null);
            result.put("authHeaderStart", authHeader != null ? authHeader.substring(0, Math.min(30, authHeader.length())) : "null");
            
            result.put("currentUserId", getCurrentUserId(request));
            result.put("timestamp", java.time.LocalDateTime.now());
            result.put("status", "success");
            
            log.info("🧪 审批API测试成功，认证状态: {}", authentication != null && authentication.isAuthenticated());
            
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            log.error("🧪 审批API测试失败", e);
            result.put("error", e.getMessage());
            result.put("status", "error");
            return ApiResponse.error("测试失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前用户待办任务
     * 替换 use-approval-management.ts 中的硬编码数据
     */
    @GetMapping("/pending")
    @ApiOperation("获取待办任务列表")
    public ApiResponse<Page<PendingTaskDTO>> getPendingTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String priority,
            @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication,
            HttpServletRequest request) {
        try {
            // 详细的认证调试信息
            log.info("🔐 JWT认证调试信息:");
            log.info("  - Authentication: {}", authentication != null ? authentication.getClass().getSimpleName() : "null");
            log.info("  - Principal: {}", authentication != null ? authentication.getPrincipal() : "null");
            log.info("  - Authorities: {}", authentication != null ? authentication.getAuthorities() : "null");
            log.info("  - Is Authenticated: {}", authentication != null ? authentication.isAuthenticated() : "false");
            
            // 检查JWT过滤器设置的用户信息
            Object userIdAttr = request.getAttribute("userId");
            Object userNameAttr = request.getAttribute("userName");
            Object userEmailAttr = request.getAttribute("userEmail");
            Object userTypeAttr = request.getAttribute("userType");
            Object departmentAttr = request.getAttribute("department");
            
            log.info("🔍 JWT过滤器设置的属性:");
            log.info("  - userId: {}", userIdAttr);
            log.info("  - userName: {}", userNameAttr);
            log.info("  - userEmail: {}", userEmailAttr);
            log.info("  - userType: {}", userTypeAttr);
            log.info("  - department: {}", departmentAttr);
            
            // 检查Authorization header
            String authHeader = request.getHeader("Authorization");
            log.info("🔑 Authorization Header: {}", authHeader != null ? 
                     "Bearer " + authHeader.substring(7, Math.min(authHeader.length(), 27)) + "..." : "null");
            
            Long currentUserId = getCurrentUserId(request);
            log.info("🎯 API调用：获取待办任务，用户ID: {}", currentUserId);
            
            Page<PendingTaskDTO> tasks = workflowIntegrationService.getPendingTasks(currentUserId, pageable);
            
            // 应用前端过滤器
            List<PendingTaskDTO> filteredTasks = tasks.getContent().stream()
                .filter(task -> {
                    if (status != null && !status.equals("all") && !status.equals(task.getStatus())) {
                        return false;
                    }
                    if (businessType != null && !businessType.equals("all") && !businessType.equals(task.getBusinessType())) {
                        return false;
                    }
                    if (priority != null && !priority.equals("all") && !priority.equals(task.getPriority())) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
            
            Page<PendingTaskDTO> filteredPage = new PageImpl<>(filteredTasks, pageable, tasks.getTotalElements());
            
            log.info("✅ 获取用户待办任务成功，用户ID: {}, 任务数量: {}", currentUserId, filteredPage.getTotalElements());
            return ApiResponse.success(filteredPage);
        } catch (Exception e) {
            log.error("❌ 获取待办任务失败", e);
            return ApiResponse.error("获取待办任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 审批通过
     * 替换前端模拟的审批操作
     */
    @PostMapping("/tasks/{taskId}/approve")
    @ApiOperation("审批通过")
    public ApiResponse<String> approveTask(
            @PathVariable String taskId,
            @RequestBody(required = false) ApprovalRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            Long currentUserId = getCurrentUserId(httpRequest);
            String comment = (request != null) ? request.getComment() : "同意";
            
            log.info("🎯 API调用：审批通过，任务ID: {}, 用户ID: {}", taskId, currentUserId);
            
            // 解析taskId，获取实际的flowable任务ID和实例ID
            PendingTaskDTO task = findTaskByTaskId(currentUserId, taskId);
            if (task == null) {
                return ApiResponse.error("任务不存在或无权限操作");
            }
            
            workflowIntegrationService.approveTask(task.getInstanceId(), task.getFlowableTaskId(), 
                    currentUserId.toString(), comment);
            
            log.info("✅ 审批通过成功，任务ID: {}, 审批人: {}", taskId, currentUserId);
            return ApiResponse.success("审批成功");
        } catch (Exception e) {
            log.error("❌ 审批通过失败，任务ID: {}", taskId, e);
            return ApiResponse.error("审批失败: " + e.getMessage());
        }
    }
    
    /**
     * 审批拒绝
     * 替换前端模拟的拒绝操作
     */
    @PostMapping("/tasks/{taskId}/reject")
    @ApiOperation("审批拒绝")
    public ApiResponse<String> rejectTask(
            @PathVariable String taskId,
            @RequestBody ApprovalRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            Long currentUserId = getCurrentUserId(httpRequest);
            
            if (request == null || request.getComment() == null || request.getComment().trim().isEmpty()) {
                return ApiResponse.error("拒绝审批必须填写意见");
            }
            
            log.info("🎯 API调用：审批拒绝，任务ID: {}, 用户ID: {}", taskId, currentUserId);
            
            // 解析taskId，获取实际的flowable任务ID和实例ID
            PendingTaskDTO task = findTaskByTaskId(currentUserId, taskId);
            if (task == null) {
                return ApiResponse.error("任务不存在或无权限操作");
            }
            
            workflowIntegrationService.rejectTask(task.getInstanceId(), task.getFlowableTaskId(), 
                    currentUserId.toString(), request.getComment());
            
            log.info("✅ 审批拒绝成功，任务ID: {}, 审批人: {}", taskId, currentUserId);
            return ApiResponse.success("拒绝成功");
        } catch (Exception e) {
            log.error("❌ 审批拒绝失败，任务ID: {}", taskId, e);
            return ApiResponse.error("拒绝失败: " + e.getMessage());
        }
    }
    
    /**
     * 打回申请
     */
    @PostMapping("/tasks/{taskId}/return")
    @ApiOperation("打回申请")
    public ApiResponse<String> returnTask(
            @PathVariable String taskId,
            @RequestBody ReturnRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            Long currentUserId = getCurrentUserId(httpRequest);
            
            if (request == null || request.getTargetNodeKey() == null || 
                request.getComment() == null || request.getComment().trim().isEmpty()) {
                return ApiResponse.error("打回申请必须指定目标节点和填写意见");
            }
            
            log.info("🎯 API调用：打回申请，任务ID: {}, 用户ID: {}", taskId, currentUserId);
            
            // 解析taskId，获取实际的flowable任务ID和实例ID
            PendingTaskDTO task = findTaskByTaskId(currentUserId, taskId);
            if (task == null) {
                return ApiResponse.error("任务不存在或无权限操作");
            }
            
            workflowIntegrationService.returnTask(task.getInstanceId(), task.getFlowableTaskId(), 
                    request.getTargetNodeKey(), currentUserId.toString(), request.getComment());
            
            log.info("✅ 打回申请成功，任务ID: {}, 目标节点: {}, 操作人: {}", 
                    taskId, request.getTargetNodeKey(), currentUserId);
            return ApiResponse.success("打回成功");
        } catch (Exception e) {
            log.error("❌ 打回申请失败，任务ID: {}", taskId, e);
            return ApiResponse.error("打回失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取已办任务列表
     * 替换前端硬编码的已办任务数据
     */
    @GetMapping("/handled")
    @ApiOperation("获取已办任务列表")
    public ApiResponse<Page<PendingTaskDTO>> getHandledTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String businessType,
            @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication,
            HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            
            Page<PendingTaskDTO> tasks = workflowIntegrationService.getHandledTasks(currentUserId, pageable);
            
            // 应用前端过滤器
            List<PendingTaskDTO> filteredTasks = tasks.getContent().stream()
                .filter(task -> {
                    if (status != null && !status.equals("all") && !status.equals(task.getStatus())) {
                        return false;
                    }
                    if (businessType != null && !businessType.equals("all") && !businessType.equals(task.getBusinessType())) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
            
            Page<PendingTaskDTO> filteredPage = new PageImpl<>(filteredTasks, pageable, tasks.getTotalElements());
            
            log.info("✅ 获取用户已办任务成功，用户ID: {}, 任务数量: {}", currentUserId, filteredPage.getTotalElements());
            return ApiResponse.success(filteredPage);
        } catch (Exception e) {
            log.error("❌ 获取已办任务失败", e);
            return ApiResponse.error("获取已办任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量审批
     * 支持批量审批和拒绝操作
     */
    @PostMapping("/batch")
    @ApiOperation("批量审批")
    public ApiResponse<BatchResult> batchApprove(
            @RequestBody BatchApprovalRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            Long currentUserId = getCurrentUserId(httpRequest);
            
            if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
                return ApiResponse.error("批量审批项目不能为空");
            }
            
            log.info("🎯 API调用：批量审批，用户ID: {}, 项目数: {}", currentUserId, request.getItems().size());
            
            // 构建批量审批请求
            List<ApprovalRequest> approvalRequests = request.getItems().stream()
                .map(item -> {
                    ApprovalRequest approvalRequest = new ApprovalRequest();
                    approvalRequest.setTaskId(item.getTaskId());
                    approvalRequest.setAction(item.getAction());
                    approvalRequest.setComment(item.getComment());
                    return approvalRequest;
                })
                .collect(Collectors.toList());
            
            BatchResult result = workflowIntegrationService.batchApprove(approvalRequests);
            
            log.info("✅ 批量审批完成，操作人: {}, 成功: {}, 失败: {}", 
                    currentUserId, result.getSuccessCount(), result.getFailureCount());
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("❌ 批量审批失败", e);
            return ApiResponse.error("批量审批失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取审批统计信息
     * 替换前端硬编码的统计数据
     */
    @GetMapping("/statistics")
    @ApiOperation("获取审批统计信息")
    public ApiResponse<ApprovalStatisticsDTO> getApprovalStatistics(
            @RequestParam(required = false, defaultValue = "today") String period,
            Authentication authentication,
            HttpServletRequest request) {
        try {
            // 认证调试信息
            log.info("🔐 统计API认证调试:");
            log.info("  - Authentication: {}", authentication != null ? "存在" : "null");
            log.info("  - Authorities: {}", authentication != null ? authentication.getAuthorities() : "null");
            
            String authHeader = request.getHeader("Authorization");
            log.info("🔑 Authorization Header: {}", authHeader != null ? "存在" : "null");
            
            Long currentUserId = getCurrentUserId(request);
            log.info("🎯 API调用：获取审批统计，用户ID: {}", currentUserId);
            
            // 获取待办任务数量
            Page<PendingTaskDTO> pendingTasks = workflowIntegrationService.getPendingTasks(currentUserId, 
                    Pageable.unpaged());
            
            // 获取已办任务数量（今日）
            Page<PendingTaskDTO> handledTasks = workflowIntegrationService.getHandledTasks(currentUserId, 
                    Pageable.unpaged());
            
            // 构建统计信息
            ApprovalStatisticsDTO statistics = new ApprovalStatisticsDTO();
            statistics.setMyPendingCount((int) pendingTasks.getTotalElements());
            
            // 统计今日已审批数量（简化实现，实际可根据时间过滤）
            long todayApproved = handledTasks.getContent().stream()
                .filter(task -> task.getApprovedTime() != null)
                .count();
            statistics.setMyApprovedToday((int) todayApproved);
            
            // 统计紧急任务数量
            long urgentTasks = pendingTasks.getContent().stream()
                .filter(task -> "high".equals(task.getPriority()))
                .count();
            statistics.setUrgentTaskCount((int) urgentTasks);
            
            // 其他统计（简化实现）
            statistics.setParallelApprovalCount(0);
            statistics.setDelegatedApprovalCount(0);
            statistics.setOverdueTaskCount(0);
            
            log.info("✅ 获取审批统计成功，用户ID: {}, 待办: {}, 今日已办: {}", 
                    currentUserId, statistics.getMyPendingCount(), statistics.getMyApprovedToday());
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            log.error("❌ 获取审批统计失败", e);
            return ApiResponse.error("获取审批统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取可打回的节点列表
     */
    @GetMapping("/tasks/{taskId}/returnable-nodes")
    @ApiOperation("获取可打回的节点列表")
    public ApiResponse<List<ReturnableNodeDTO>> getReturnableNodes(
            @PathVariable String taskId,
            Authentication authentication,
            HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            
            // 查找任务
            PendingTaskDTO task = findTaskByTaskId(currentUserId, taskId);
            if (task == null) {
                return ApiResponse.error("任务不存在或无权限操作");
            }
            
            // 获取可打回节点
            List<java.util.Map<String, String>> returnableNodes = workflowIntegrationService
                .getReturnableNodes(task.getInstanceId());
            
            // 转换为DTO
            List<ReturnableNodeDTO> nodes = returnableNodes.stream()
                .map(node -> {
                    ReturnableNodeDTO dto = new ReturnableNodeDTO();
                    dto.setNodeKey(node.get("nodeKey"));
                    dto.setNodeName(node.get("nodeName"));
                    dto.setAssigneeName(node.get("assigneeName"));
                    dto.setApprovedTime(node.get("approvedTime"));
                    return dto;
                })
                .collect(Collectors.toList());
            
            return ApiResponse.success(nodes);
        } catch (Exception e) {
            log.error("❌ 获取可打回节点失败，任务ID: {}", taskId, e);
            return ApiResponse.error("获取可打回节点失败: " + e.getMessage());
        }
    }
    
    // 辅助方法
    
    /**
     * 从JWT认证信息中获取当前用户ID
     * 真正的JWT认证实现
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        // 优先尝试从传统JWT过滤器设置的request attribute中获取用户ID
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj != null) {
            Long userId = (Long) userIdObj;
            log.debug("从传统JWT获取用户ID: {}", userId);
            return userId;
        }
        
        // 如果是Keycloak认证，从Keycloak JWT服务获取用户信息
        if (keycloakJwtAuthenticationService != null) {
            try {
                Optional<User> currentUser = keycloakJwtAuthenticationService.getCurrentUser();
                if (currentUser.isPresent()) {
                    Long userId = currentUser.get().getId();
                    log.debug("从Keycloak JWT获取用户ID: {}", userId);
                    return userId;
                }
            } catch (Exception e) {
                log.error("从Keycloak获取用户信息失败", e);
            }
        }
        
        // 如果没有认证信息，抛出异常
        log.error("无法获取用户认证信息，请检查JWT Token");
        throw new RuntimeException("用户未认证，请重新登录");
    }
    

    
    /**
     * 根据费用申请ID查找待办任务
     * 🔧 修正：taskId现在是费用申请ID，需要通过这个ID找到对应的工作流任务
     */
    private PendingTaskDTO findTaskByTaskId(Long userId, String expenseApplicationId) {
        try {
            Page<PendingTaskDTO> tasks = workflowIntegrationService.getPendingTasks(userId, Pageable.unpaged());
            return tasks.getContent().stream()
                .filter(task -> expenseApplicationId.equals(task.getTaskId()) || 
                               expenseApplicationId.equals(task.getBusinessId()))
                .findFirst()
                .orElse(null);
        } catch (Exception e) {
            log.error("❌ 查找任务失败，用户ID: {}, 费用申请ID: {}", userId, expenseApplicationId, e);
            return null;
        }
    }
} 