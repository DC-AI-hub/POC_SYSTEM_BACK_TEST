package demo.backed.controller;

import demo.backed.dto.*;
import demo.backed.entity.WorkflowInstance;
import demo.backed.entity.WorkflowNode;
import demo.backed.repository.WorkflowInstanceRepository;
import demo.backed.repository.WorkflowNodeRepository;
import demo.backed.service.WorkflowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/workflow")
@Api(tags = "工作流管理")
@Slf4j
public class WorkflowController {
    
    @Autowired
    private WorkflowService workflowService;
    
    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;
    
    @Autowired
    private WorkflowNodeRepository workflowNodeRepository;
    
    @Autowired
    private RuntimeService runtimeService;
    
    /**
     * 启动工作流
     */
    @PostMapping("/instances")
    @ApiOperation("启动工作流")
    public ApiResponse<WorkflowInstanceDTO> startWorkflow(@Valid @RequestBody StartProcessRequest request) {
        try {
            // 设置申请人信息
            if (request.getApplicantId() == null) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof UserDTO) {
                    UserDTO currentUser = (UserDTO) auth.getPrincipal();
                    request.setApplicantId(currentUser.getId());
                }
            }
            
            WorkflowInstanceDTO instance = workflowService.startWorkflow(request);
            return ApiResponse.success("工作流启动成功", instance);
        } catch (Exception e) {
            log.error("启动工作流失败", e);
            return ApiResponse.error("启动工作流失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取流程实例详情
     */
    @GetMapping("/instances/{id}")
    @ApiOperation("获取流程实例详情")
    public ApiResponse<WorkflowInstanceDTO> getWorkflowInstance(@PathVariable Long id) {
        try {
            WorkflowInstanceDTO instance = workflowService.getWorkflowInstance(id);
            return ApiResponse.success(instance);
        } catch (Exception e) {
            log.error("获取流程实例失败", e);
            return ApiResponse.error("获取流程实例失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取流程实例列表
     */
    @GetMapping("/instances")
    @ApiOperation("获取流程实例列表")
    public ApiResponse<Page<WorkflowInstanceDTO>> getWorkflowInstances(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long applicantId,
            @PageableDefault(size = 10) Pageable pageable) {
        try {
            Page<WorkflowInstanceDTO> instances = workflowService.getWorkflowInstances(status, applicantId, pageable);
            return ApiResponse.success(instances);
        } catch (Exception e) {
            log.error("获取流程实例列表失败", e);
            return ApiResponse.error("获取流程实例列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 审批通过
     */
    @PostMapping("/instances/{id}/approve")
    @ApiOperation("审批通过")
    public ApiResponse<Void> approve(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalRequest request) {
        try {
            // 获取Flowable任务ID
            WorkflowNode node = workflowNodeRepository.findById(request.getTaskId())
                .orElseThrow(() -> new RuntimeException("任务不存在"));
            
            workflowService.approve(id, node.getTaskId(), request.getComment());
            return ApiResponse.success("审批成功", null);
        } catch (Exception e) {
            log.error("审批失败", e);
            return ApiResponse.error("审批失败: " + e.getMessage());
        }
    }
    
    /**
     * 审批拒绝
     */
    @PostMapping("/instances/{id}/reject")
    @ApiOperation("审批拒绝")
    public ApiResponse<Void> reject(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalRequest request) {
        try {
            WorkflowNode node = workflowNodeRepository.findById(request.getTaskId())
                .orElseThrow(() -> new RuntimeException("任务不存在"));
            
            workflowService.reject(id, node.getTaskId(), request.getComment());
            return ApiResponse.success("拒绝成功", null);
        } catch (Exception e) {
            log.error("拒绝失败", e);
            return ApiResponse.error("拒绝失败: " + e.getMessage());
        }
    }
    
    /**
     * 打回至指定节点
     */
    @PostMapping("/instances/{id}/return")
    @ApiOperation("打回至指定节点")
    public ApiResponse<Void> returnTo(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalRequest request) {
        try {
            if (request.getTargetNodeKey() == null) {
                return ApiResponse.error("目标节点不能为空");
            }
            
            WorkflowNode node = workflowNodeRepository.findById(request.getTaskId())
                .orElseThrow(() -> new RuntimeException("任务不存在"));
            
            workflowService.returnTo(id, node.getTaskId(), request.getTargetNodeKey(), request.getComment());
            return ApiResponse.success("打回成功", null);
        } catch (Exception e) {
            log.error("打回失败", e);
            return ApiResponse.error("打回失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取待办任务
     */
    @GetMapping("/pending/{userId}")
    @ApiOperation("获取待办任务")
    public ApiResponse<Page<PendingTaskDTO>> getPendingTasks(
            @PathVariable Long userId,
            @PageableDefault(size = 10) Pageable pageable) {
        try {
            Page<PendingTaskDTO> tasks = workflowService.getPendingTasks(userId, pageable);
            return ApiResponse.success(tasks);
        } catch (Exception e) {
            log.error("获取待办任务失败", e);
            return ApiResponse.error("获取待办任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取已办任务
     */
    @GetMapping("/handled/{userId}")
    @ApiOperation("获取已办任务")
    public ApiResponse<Page<PendingTaskDTO>> getHandledTasks(
            @PathVariable Long userId,
            @PageableDefault(size = 10) Pageable pageable) {
        try {
            Page<PendingTaskDTO> tasks = workflowService.getHandledTasks(userId, pageable);
            return ApiResponse.success(tasks);
        } catch (Exception e) {
            log.error("获取已办任务失败", e);
            return ApiResponse.error("获取已办任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取审批历史
     */
    @GetMapping("/instances/{id}/history")
    @ApiOperation("获取审批历史")
    public ApiResponse<List<Map<String, Object>>> getHistory(@PathVariable Long id) {
        try {
            WorkflowInstance instance = workflowInstanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("流程实例不存在"));
            
            List<Map<String, Object>> history = workflowService.getDetailedHistory(instance.getProcessInstanceId());
            return ApiResponse.success(history);
        } catch (Exception e) {
            log.error("获取审批历史失败", e);
            return ApiResponse.error("获取审批历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取可打回节点列表
     */
    @GetMapping("/nodes/{nodeId}/next-nodes")
    @ApiOperation("获取可打回节点列表")
    public ApiResponse<List<Map<String, String>>> getNextNodes(@PathVariable Long nodeId) {
        try {
            List<Map<String, String>> nodes = workflowService.getReturnableNodes(nodeId);
            return ApiResponse.success(nodes);
        } catch (Exception e) {
            log.error("获取可打回节点失败", e);
            return ApiResponse.error("获取可打回节点失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量审批
     */
    @PostMapping("/batch-approve")
    @ApiOperation("批量审批")
    public ApiResponse<BatchResult> batchApprove(@Valid @RequestBody List<ApprovalRequest> requests) {
        try {
            BatchResult result = workflowService.batchApprove(requests);
            return ApiResponse.success("批量审批完成", result);
        } catch (Exception e) {
            log.error("批量审批失败", e);
            return ApiResponse.error("批量审批失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理测试数据
     */
    @DeleteMapping("/test/cleanup/{businessId}")
    @ApiOperation("清理测试数据")
    @Transactional
    public ApiResponse<String> cleanupTestData(@PathVariable String businessId) {
        try {
            log.info("清理业务ID为 {} 的测试数据", businessId);
            
            // 查找并删除相关的工作流实例
            Optional<WorkflowInstance> instance = workflowInstanceRepository
                .findByBusinessTypeAndBusinessId("EXPENSE", businessId);
            
            if (instance.isPresent()) {
                WorkflowInstance workflowInstance = instance.get();
                
                // 如果流程还在运行，先终止流程
                if ("RUNNING".equals(workflowInstance.getStatus()) && 
                    workflowInstance.getProcessInstanceId() != null) {
                    try {
                        runtimeService.deleteProcessInstance(
                            workflowInstance.getProcessInstanceId(), 
                            "清理测试数据"
                        );
                    } catch (Exception e) {
                        log.warn("终止流程实例失败: {}", e.getMessage());
                    }
                }
                
                // 删除工作流节点
                workflowNodeRepository.deleteByInstanceId(workflowInstance.getId());
                
                // 删除工作流实例
                workflowInstanceRepository.delete(workflowInstance);
                workflowInstanceRepository.flush();
                
                log.info("成功清理业务ID {} 的测试数据", businessId);
                return ApiResponse.success("清理成功");
            } else {
                log.info("未找到业务ID {} 的工作流实例", businessId);
                return ApiResponse.success("未找到相关数据");
            }
        } catch (Exception e) {
            log.error("清理测试数据失败", e);
            return ApiResponse.error("清理失败: " + e.getMessage());
        }
    }
} 