package demo.backed.service;

import demo.backed.dto.*;
import demo.backed.entity.WorkflowInstance;
import demo.backed.entity.WorkflowNode;
import demo.backed.entity.WorkflowTemplate;
import demo.backed.repository.WorkflowInstanceRepository;
import demo.backed.repository.WorkflowNodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.engine.task.Comment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
@Transactional
@Slf4j
public class WorkflowService {
    
    @Autowired
    private RuntimeService runtimeService;
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private HistoryService historyService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private WorkflowInstanceRepository workflowInstanceRepository;
    
    @Autowired
    private WorkflowNodeRepository workflowNodeRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private WorkflowTemplateService workflowTemplateService;
    
    @Autowired
    private RepositoryService repositoryService;
    
    /**
     * 启动工作流
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowInstanceDTO startWorkflow(StartProcessRequest request) {
        try {
            // 创建并保存工作流实例
            WorkflowInstance instance = new WorkflowInstance();
            instance.setBusinessType(request.getBusinessType());
            instance.setBusinessId(request.getBusinessId());
            instance.setTitle(request.getTitle());
            instance.setApplicantId(request.getApplicantId());
            instance.setStartTime(LocalDateTime.now());
            instance.setStatus("CREATED");
            
            // 先保存获取ID，作为占位，避免并发问题
            instance = workflowInstanceRepository.save(instance);
            workflowInstanceRepository.flush();
            
            try {
            // 获取申请人信息
            UserDTO applicant = userService.getUserById(request.getApplicantId())
                .orElseThrow(() -> new RuntimeException("申请人不存在"));
            
                // 更新申请人姓名
                instance.setApplicantName(applicant.getUserName());
                
            // 准备流程变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("applicantId", request.getApplicantId());
            variables.put("applicantName", applicant.getUserName());
                variables.put("department", applicant.getDepartment());
            variables.put("managerId", getManagerId(applicant));
                variables.put("financeManagerId", getFinanceManagerId());
                variables.put("complianceManagerId", getComplianceManagerId());
            variables.put("functionalHeadId", getFunctionalHeadId(applicant));
                variables.put("executiveId", getExecutiveId(request.getAmount()));
            variables.put("amount", request.getAmount());
            variables.put("businessType", request.getBusinessType());
            variables.put("businessId", request.getBusinessId());
            
            if (request.getVariables() != null) {
                variables.putAll(request.getVariables());
            }
            
            // 启动Flowable流程实例
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                getProcessDefinitionKey(request.getBusinessType()),
                request.getBusinessId(),
                variables
            );
            
                // 更新流程实例信息
            instance.setProcessInstanceId(processInstance.getId());
            instance.setStatus("RUNNING");
            instance.setVariables(objectMapper.writeValueAsString(variables));
            
            // 获取当前任务并创建节点记录
            Task currentTask = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();
            
            if (currentTask != null) {
                instance.setCurrentNodeName(currentTask.getName());
                instance.setCurrentAssignee(currentTask.getAssignee());
                createWorkflowNode(instance, currentTask);
            }
            
                // 更新实例信息（不是重新保存）
                instance = workflowInstanceRepository.save(instance);
                
                log.info("启动工作流成功，流程实例ID: {}, 业务ID: {}", processInstance.getId(), request.getBusinessId());
            
            return convertToDTO(instance);
                
            } catch (Exception e) {
                // 如果Flowable启动失败，删除占位记录
                log.error("启动Flowable流程失败，删除占位记录", e);
                workflowInstanceRepository.delete(instance);
                workflowInstanceRepository.flush();
                throw e;
            }
            
        } catch (Exception e) {
            log.error("启动工作流失败", e);
            // 如果是约束冲突，尝试清理并重试
            if (e.getMessage() != null && e.getMessage().contains("constraint")) {
                // 尝试查找并返回已存在的实例
                Optional<WorkflowInstance> existing = workflowInstanceRepository
                    .findByBusinessTypeAndBusinessId(request.getBusinessType(), request.getBusinessId());
                if (existing.isPresent()) {
                    log.warn("发现已存在的流程实例，返回现有实例");
                    return convertToDTO(existing.get());
                }
            }
            throw new RuntimeException("启动工作流失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取待办任务列表
     */
    public Page<PendingTaskDTO> getPendingTasks(Long userId, Pageable pageable) {
        try {
            log.info("🔍 开始获取用户待办任务，用户ID: {}", userId);
            
            // 参数验证
            if (userId == null) {
                log.error("❌ 用户ID为空");
                return new PageImpl<>(new ArrayList<>(), pageable != null ? pageable : PageRequest.of(0, 20), 0);
            }
            
            if (pageable == null) {
                pageable = PageRequest.of(0, 20);
            }
            
            // 处理Unpaged情况
            int pageNumber = pageable.isPaged() ? pageable.getPageNumber() : 0;
            int pageSize = pageable.isPaged() ? pageable.getPageSize() : 20;
            
            // 获取用户待办任务
            List<Task> tasks = taskService.createTaskQuery()
                .taskCandidateOrAssigned(String.valueOf(userId))
                .orderByTaskCreateTime().desc()
                .listPage(pageNumber * pageSize, pageSize);
            
            long total = taskService.createTaskQuery()
                .taskCandidateOrAssigned(String.valueOf(userId))
                .count();
            
            log.info("📊 Flowable查询结果: 用户 {} 的待办任务数量: {}, 总数: {}", userId, tasks.size(), total);
            
            // 转换为DTO
            List<PendingTaskDTO> dtos = tasks.stream().map(task -> {
            PendingTaskDTO dto = new PendingTaskDTO();
            
            // 核心任务信息 - 使用前端期望的字段名
            dto.setFlowableTaskId(task.getId());
            dto.setProcessInstanceId(task.getProcessInstanceId());
            dto.setTaskName(task.getName());              // 前端期望的字段名
            dto.setCurrentNodeName(task.getName());       // 保留兼容性
            dto.setSubmitTime(task.getCreateTime());
            dto.setCreateTime(task.getCreateTime().toString()); // 前端期望的字段名
            
            // 获取流程变量
            Map<String, Object> vars = runtimeService.getVariables(task.getProcessInstanceId());
            dto.setApplicantName((String) vars.get("applicantName"));
            dto.setBusinessType((String) vars.get("businessType"));
            
            // 🔧 优先从流程变量获取费用申请ID，如果没有则使用businessId
            Object applicationIdVar = vars.get("applicationId");
            if (applicationIdVar != null) {
                dto.setTaskId(applicationIdVar.toString());     // taskId = 费用申请ID
                dto.setBusinessId(applicationIdVar.toString()); // businessId = 费用申请ID
            } else {
                dto.setBusinessId((String) vars.get("businessId"));
                dto.setTaskId((String) vars.get("businessId"));
            }
            
            // 金额处理 - 转换为前端期望的Double类型
            Object amountObj = vars.get("amount");
            if (amountObj != null) {
                dto.setOriginalAmount((BigDecimal) amountObj);  // 保留原始值
                dto.setAmount(((BigDecimal) amountObj).doubleValue()); // 前端期望的number类型
            }
            
            // 查询工作流实例
            WorkflowInstance instance = workflowInstanceRepository
                .findByProcessInstanceId(task.getProcessInstanceId())
                .orElse(null);
            
            if (instance != null) {
                dto.setInstanceId(instance.getId());
                dto.setTitle(instance.getTitle());
                // 🔧 修正：从WorkflowInstance补充ID信息（如果流程变量中没有的话）
                if (dto.getTaskId() == null) {
                    dto.setTaskId(instance.getBusinessId());       // taskId = 费用申请ID
                }
                if (dto.getBusinessId() == null) {
                    dto.setBusinessId(instance.getBusinessId());   // businessId = 费用申请ID
                }
                dto.setApplicationNumber((String) vars.get("applicationNumber")); // 申请单号从变量获取
                
                // 查询申请人详细信息获取部门
                if (instance.getApplicantId() != null) {
                    Optional<UserDTO> applicantUser = userService.getUserById(instance.getApplicantId());
                    if (applicantUser.isPresent()) {
                        dto.setDepartment(applicantUser.get().getDepartment());
                    }
                }
            }
            
            // 查询工作流节点
            WorkflowNode node = workflowNodeRepository
                .findByTaskId(task.getId())
                .orElse(null);
            
            if (node != null) {
                // 🔧 移除错误的taskId设置，已在上方通过instance.businessId设置
                dto.setStatus(mapNodeStatusToTaskStatus(node.getStatus())); // 映射状态
                dto.setPriority("medium"); // 默认中等优先级（WorkflowNode暂无priority字段）
                
                // 委托信息
                boolean isDelegated = node.getProxyId() != null && node.getProxyId().equals(userId);
                dto.setDelegated(isDelegated);        // 前端期望的字段名
                dto.setIsProxy(isDelegated);           // 保留兼容性
                
                // 审批人信息
                if (node.getAssigneeId() != null) {
                    Optional<UserDTO> assigneeUser = userService.getUserById(node.getAssigneeId());
                    if (assigneeUser.isPresent()) {
                        dto.setAssignee(assigneeUser.get().getUserName());
                    }
                } else {
                    dto.setAssignee("待分配");
                }
            } else {
                // 如果没有WorkflowNode记录，使用默认值
                // 🔧 不再重新设置taskId，保持费用申请ID
                dto.setStatus("pending");
                dto.setPriority("medium");
                dto.setDelegated(false);
                dto.setIsProxy(false);
                dto.setAssignee(task.getAssignee() != null ? 
                    getUserNameById(task.getAssignee()) : "待分配");
            }
            
            // 设置默认值
            dto.setAttachmentCount(0); // 暂时设为0，后续可查询实际附件数量
            dto.setDescription(dto.getTitle()); // 使用标题作为描述
            
            return dto;
        }).collect(Collectors.toList());
        
        log.info("✅ 成功转换 {} 个任务为DTO", dtos.size());
        return new PageImpl<>(dtos, pageable, total);
        
        } catch (Exception e) {
            log.error("❌ 获取待办任务失败，用户ID: {}", userId, e);
            // 返回空的Page对象，避免前端获取null
            return new PageImpl<>(new ArrayList<>(), pageable != null ? pageable : Pageable.unpaged(), 0);
        }
    }
    
    /**
     * 获取已办任务列表
     */
    public Page<PendingTaskDTO> getHandledTasks(Long userId, Pageable pageable) {
        try {
            log.info("🔍 开始获取用户已办任务，用户ID: {}", userId);
            
            // 参数验证
            if (userId == null) {
                log.warn("⚠️ 用户ID为空，返回空的已办任务列表");
                return new PageImpl<>(new ArrayList<>(), pageable != null ? pageable : PageRequest.of(0, 20), 0);
            }
            
            if (pageable == null) {
                pageable = PageRequest.of(0, 20);
            }
            
            // 检查historyService是否可用
            if (historyService == null) {
                log.error("❌ historyService未正确注入");
                return new PageImpl<>(new ArrayList<>(), pageable, 0);
            }
            
            // 处理Unpaged情况
            int pageNumber = pageable.isPaged() ? pageable.getPageNumber() : 0;
            int pageSize = pageable.isPaged() ? pageable.getPageSize() : 20;
            
            // 查询用户已完成的历史任务
            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(String.valueOf(userId))
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .listPage(pageNumber * pageSize, pageSize);
            
            if (historicTasks == null) {
                log.warn("⚠️ 历史任务查询返回null，返回空列表");
                return new PageImpl<>(new ArrayList<>(), pageable, 0);
            }
            
            long total = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(String.valueOf(userId))
                .finished()
                .count();
            
            log.info("📋 查询到用户已办任务数量: {}, 总数: {}", historicTasks.size(), total);
            
            // 转换为DTO
            List<PendingTaskDTO> dtos = historicTasks.stream().map(task -> {
            PendingTaskDTO dto = new PendingTaskDTO();
            dto.setFlowableTaskId(task.getId());
            dto.setProcessInstanceId(task.getProcessInstanceId());
            dto.setCurrentNodeName(task.getName());
            dto.setSubmitTime(task.getCreateTime());
            dto.setApprovedTime(task.getEndTime());
            
            // 查询工作流实例
            WorkflowInstance instance = workflowInstanceRepository
                .findByProcessInstanceId(task.getProcessInstanceId())
                .orElse(null);
            
            if (instance != null) {
                dto.setInstanceId(instance.getId());
                dto.setTitle(instance.getTitle());
                dto.setApplicantName(instance.getApplicantName());
                dto.setBusinessType(instance.getBusinessType());
                dto.setBusinessId(instance.getBusinessId());
                
                // 尝试从变量中获取金额
                try {
                    String variablesJson = instance.getVariables();
                    if (variablesJson != null) {
                        Map<String, Object> variables = objectMapper.readValue(variablesJson, 
                            new TypeReference<Map<String, Object>>() {});
                        if (variables.containsKey("amount")) {
                            BigDecimal amountBD = new BigDecimal(variables.get("amount").toString());
                            dto.setOriginalAmount(amountBD);  // 保存原始BigDecimal
                            dto.setAmount(amountBD.doubleValue()); // 转换为Double
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析流程变量失败: {}", e.getMessage());
                }
            }
            
            // 查询历史节点记录
            List<WorkflowNode> nodes = workflowNodeRepository.findByInstanceId(instance != null ? instance.getId() : null);
            WorkflowNode historicNode = nodes.stream()
                .filter(n -> n.getTaskId().equals(task.getId()))
                .findFirst()
                .orElse(null);
            
            if (historicNode != null) {
                dto.setTaskId(String.valueOf(historicNode.getId())); // 转换为String类型
                dto.setComment(historicNode.getComment());
                dto.setStatus(historicNode.getStatus());
            }
            
            return dto;
        }).collect(Collectors.toList());
        
        log.info("✅ 成功转换 {} 个已办任务为DTO", dtos.size());
        return new PageImpl<>(dtos, pageable, total);
        
        } catch (Exception e) {
            log.error("❌ 获取已办任务失败，用户ID: {}", userId, e);
            // 返回空的Page对象，避免前端获取null
            return new PageImpl<>(new ArrayList<>(), pageable != null ? pageable : Pageable.unpaged(), 0);
        }
    }
    
    /**
     * 审批通过
     */
    public void approve(Long instanceId, String taskId, String comment) {
        try {
            Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();
            
            if (task == null) {
                throw new RuntimeException("任务不存在");
            }
            
            // 添加审批意见
            taskService.addComment(taskId, task.getProcessInstanceId(), comment);
            
            // 更新工作流节点
            WorkflowNode node = workflowNodeRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("工作流节点不存在"));
            
            node.setStatus("COMPLETED");
            node.setComment(comment);
            node.setApprovedTime(LocalDateTime.now());
            workflowNodeRepository.save(node);
            
            // 完成任务
            taskService.complete(taskId);
            
            // 更新流程实例状态
            updateWorkflowInstanceStatus(task.getProcessInstanceId());
            
            log.info("审批通过，任务ID: {}, 审批意见: {}", taskId, comment);
            
        } catch (Exception e) {
            log.error("审批失败", e);
            throw new RuntimeException("审批失败: " + e.getMessage());
        }
    }
    
    /**
     * 审批拒绝
     */
    public void reject(Long instanceId, String taskId, String comment) {
        try {
            Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();
            
            if (task == null) {
                throw new RuntimeException("任务不存在");
            }
            
            // 添加审批意见
            taskService.addComment(taskId, task.getProcessInstanceId(), comment);
            
            // 更新工作流节点
            WorkflowNode node = workflowNodeRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("工作流节点不存在"));
            
            node.setStatus("REJECTED");
            node.setComment(comment);
            node.setApprovedTime(LocalDateTime.now());
            workflowNodeRepository.save(node);
            
            // 终止流程
            runtimeService.deleteProcessInstance(task.getProcessInstanceId(), "拒绝: " + comment);
            
            // 更新流程实例状态
            WorkflowInstance instance = workflowInstanceRepository
                .findByProcessInstanceId(task.getProcessInstanceId())
                .orElseThrow(() -> new RuntimeException("流程实例不存在"));
            
            instance.setStatus("REJECTED");
            instance.setEndTime(LocalDateTime.now());
            workflowInstanceRepository.save(instance);
            
            log.info("审批拒绝，任务ID: {}, 审批意见: {}", taskId, comment);
            
        } catch (Exception e) {
            log.error("拒绝失败", e);
            throw new RuntimeException("拒绝失败: " + e.getMessage());
        }
    }
    
    /**
     * 打回至指定节点
     */
    public void returnTo(Long instanceId, String taskId, String targetNodeKey, String comment) {
        try {
            Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();
            
            if (task == null) {
                throw new RuntimeException("任务不存在");
            }
            
            // 添加审批意见
            taskService.addComment(taskId, task.getProcessInstanceId(), "打回: " + comment);
            
            // 更新工作流节点
            WorkflowNode node = workflowNodeRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("工作流节点不存在"));
            
            node.setStatus("RETURNED");
            node.setComment("打回: " + comment);
            node.setApprovedTime(LocalDateTime.now());
            node.setIsReturned(true);
            workflowNodeRepository.save(node);
            
            // 使用Flowable的ChangeActivityState API
            runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(task.getProcessInstanceId())
                .moveActivityIdTo(task.getTaskDefinitionKey(), targetNodeKey)
                .changeState();
            
            log.info("打回操作，任务ID: {}, 目标节点: {}", taskId, targetNodeKey);
            
        } catch (Exception e) {
            log.error("打回失败", e);
            throw new RuntimeException("打回失败: " + e.getMessage());
        }
    }
    

    
    /**
     * 获取流程实例详情
     */
    public WorkflowInstanceDTO getWorkflowInstance(Long instanceId) {
        WorkflowInstance instance = workflowInstanceRepository.findById(instanceId)
            .orElseThrow(() -> new RuntimeException("流程实例不存在"));
        
        WorkflowInstanceDTO dto = convertToDTO(instance);
        
        // 获取流程变量
        if ("RUNNING".equals(instance.getStatus())) {
            Map<String, Object> variables = runtimeService.getVariables(instance.getProcessInstanceId());
            dto.setVariables(variables);
        }
        
        return dto;
    }
    
    /**
     * 获取审批历史（返回详细信息）
     */
    public List<Map<String, Object>> getDetailedHistory(String processInstanceId) {
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
            .processInstanceId(processInstanceId)
            .orderByHistoricTaskInstanceStartTime().asc()
            .list();
        
        List<Map<String, Object>> history = new ArrayList<>();
        
        for (HistoricTaskInstance task : historicTasks) {
            Map<String, Object> taskInfo = new HashMap<>();
            taskInfo.put("taskId", task.getId());
            taskInfo.put("taskName", task.getName());
            taskInfo.put("assignee", task.getAssignee());
            taskInfo.put("startTime", task.getCreateTime());
            taskInfo.put("endTime", task.getEndTime());
            taskInfo.put("duration", task.getDurationInMillis());
            taskInfo.put("deleteReason", task.getDeleteReason());
            
            // 查询审批意见
            List<Comment> comments = taskService.getTaskComments(task.getId());
            if (!comments.isEmpty()) {
                taskInfo.put("comment", comments.get(0).getFullMessage());
            }
            
            // 查询审批人信息
            if (task.getAssignee() != null) {
                try {
                    Long assigneeId = Long.valueOf(task.getAssignee());
                    Optional<UserDTO> assigneeUser = userService.getUserById(assigneeId);
                    if (assigneeUser.isPresent()) {
                        taskInfo.put("assigneeName", assigneeUser.get().getUserName());
                        taskInfo.put("assigneeDepartment", assigneeUser.get().getDepartment());
                    }
                } catch (NumberFormatException e) {
                    log.warn("无法解析审批人ID: {}", task.getAssignee());
                }
            }
            
            // 从我们的节点表获取额外信息
            WorkflowInstance instance = workflowInstanceRepository
                .findByProcessInstanceId(processInstanceId)
                .orElse(null);
            
            if (instance != null) {
                List<WorkflowNode> nodes = workflowNodeRepository.findByInstanceId(instance.getId());
                WorkflowNode node = nodes.stream()
                    .filter(n -> n.getTaskId().equals(task.getId()))
                    .findFirst()
                    .orElse(null);
                
                if (node != null) {
                    taskInfo.put("nodeStatus", node.getStatus());
                    taskInfo.put("nodeComment", node.getComment());
                    taskInfo.put("approvedTime", node.getApprovedTime());
                }
            }
            
            history.add(taskInfo);
        }
        
        return history;
    }
    
    /**
     * 批量审批
     */
    @Transactional
    public BatchResult batchApprove(List<ApprovalRequest> requests) {
        BatchResult result = new BatchResult();
        
        for (ApprovalRequest request : requests) {
            try {
                WorkflowNode node = workflowNodeRepository.findById(request.getTaskId())
                    .orElseThrow(() -> new RuntimeException("任务不存在"));
                
                if ("approve".equals(request.getAction())) {
                    approve(node.getInstanceId(), node.getTaskId(), request.getComment());
                } else if ("reject".equals(request.getAction())) {
                    reject(node.getInstanceId(), node.getTaskId(), request.getComment());
                }
                result.addSuccess(request.getTaskId());
            } catch (Exception e) {
                result.addFailure(request.getTaskId(), "任务" + request.getTaskId() + ": " + e.getMessage());
            }
        }
        
        return result;
    }
    
    /**
     * 获取流程实例列表
     */
    public Page<WorkflowInstanceDTO> getWorkflowInstances(String status, Long applicantId, Pageable pageable) {
        // 构建查询条件
        Page<WorkflowInstance> instances;
        
        if (status != null && applicantId != null) {
            instances = workflowInstanceRepository.findByStatusAndApplicantId(status, applicantId, pageable);
        } else if (status != null) {
            instances = workflowInstanceRepository.findByStatus(status, pageable);
        } else if (applicantId != null) {
            instances = workflowInstanceRepository.findByApplicantId(applicantId, pageable);
        } else {
            instances = workflowInstanceRepository.findAll(pageable);
        }
        
        // 转换为DTO
        List<WorkflowInstanceDTO> dtos = instances.getContent().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return new PageImpl<>(dtos, pageable, instances.getTotalElements());
    }
    
    /**
     * 获取可打回的节点列表
     */
    public List<Map<String, String>> getReturnableNodes(Long nodeId) {
        WorkflowNode currentNode = workflowNodeRepository.findById(nodeId)
            .orElseThrow(() -> new RuntimeException("节点不存在"));
        
        WorkflowInstance instance = workflowInstanceRepository.findById(currentNode.getInstanceId())
            .orElseThrow(() -> new RuntimeException("流程实例不存在"));
        
        // 查询该流程实例的所有已完成节点
        List<WorkflowNode> completedNodes = workflowNodeRepository
            .findByInstanceIdAndStatus(instance.getId(), "COMPLETED");
        
        // 构建可打回节点列表（只能打回到之前的节点）
        List<Map<String, String>> returnableNodes = new ArrayList<>();
        
        for (WorkflowNode node : completedNodes) {
            // 排除自己和之后的节点
            if (node.getId() < currentNode.getId()) {
                Map<String, String> nodeInfo = new HashMap<>();
                nodeInfo.put("nodeKey", node.getNodeKey());
                nodeInfo.put("nodeName", node.getNodeName());
                nodeInfo.put("assigneeName", node.getAssigneeName());
                nodeInfo.put("approvedTime", node.getApprovedTime() != null ? 
                    node.getApprovedTime().toString() : "");
                returnableNodes.add(nodeInfo);
            }
        }
        
        return returnableNodes;
    }
    
    // 辅助方法
    
    private String getProcessDefinitionKey(String businessType) {
        try {
            log.info("🔍 查找业务类型 {} 的动态工作流模板", businessType);
            
            // 1. 使用WorkflowTemplateService的专用方法查找已部署的模板
            Optional<WorkflowTemplate> matchedTemplate = workflowTemplateService
                .findDeployedTemplateByBusinessType(businessType);
            
            if (matchedTemplate.isPresent()) {
                WorkflowTemplate template = matchedTemplate.get();
                
                // 验证模板是否在Flowable中真正可用
                boolean isAvailable = workflowTemplateService.isTemplateAvailable(template.getProcessKey());
                if (isAvailable) {
                    // 检查是否需要使用兼容模式（使用expenseApproval key）
                    long exactCount = repositoryService.createProcessDefinitionQuery()
                        .processDefinitionKey(template.getProcessKey())
                        .active()
                        .count();
                    
                    if (exactCount > 0) {
                        log.info("✅ 找到并验证动态工作流模板: {} (processKey: {})", 
                                template.getName(), template.getProcessKey());
                        return template.getProcessKey();
                    } else {
                        // 使用兼容模式
                        log.info("✅ 使用兼容模式的工作流模板: {} (使用expenseApproval)", template.getName());
                        return "expenseApproval";
                    }
                } else {
                    log.warn("⚠️ 工作流模板在数据库中存在但在Flowable中不可用: {}", template.getProcessKey());
                }
            }
            
            // 2. 查找默认的费用审批模板
            if (businessType.toUpperCase().contains("EXPENSE")) {
                Optional<WorkflowTemplate> defaultTemplate = workflowTemplateService.getDefaultExpenseTemplate();
                if (defaultTemplate.isPresent()) {
                    WorkflowTemplate template = defaultTemplate.get();
                    boolean isAvailable = workflowTemplateService.isTemplateAvailable(template.getProcessKey());
                    if (isAvailable) {
                        log.info("✅ 使用默认费用审批工作流模板: {} (processKey: {})", 
                                template.getName(), template.getProcessKey());
                        return template.getProcessKey();
                    }
                }
            }
            
            // 3. 查找任何可用的已部署模板
            List<WorkflowTemplate> allDeployedTemplates = workflowTemplateService.getAllTemplates().stream()
                .filter(template -> template.getIsDeployed())
                .filter(template -> "active".equals(template.getStatus()))
                .collect(Collectors.toList());
            
            for (WorkflowTemplate template : allDeployedTemplates) {
                boolean isAvailable = workflowTemplateService.isTemplateAvailable(template.getProcessKey());
                if (isAvailable) {
                    log.info("✅ 使用可用的工作流模板: {} (processKey: {})", 
                            template.getName(), template.getProcessKey());
                    return template.getProcessKey();
                }
            }
            
            // 4. 如果完全没有动态模板，抛出详细的异常信息
            log.error("❌ 没有找到任何可用的动态工作流模板！");
            log.error("   业务类型: {}", businessType);
            log.error("   所有模板数量: {}", workflowTemplateService.getAllTemplates().size());
            log.error("   已部署模板数量: {}", allDeployedTemplates.size());
            
            // 记录所有可用模板的详细信息
            List<WorkflowTemplate> allTemplates = workflowTemplateService.getAllTemplates();
            log.error("   所有工作流模板详情:");
            for (WorkflowTemplate template : allTemplates) {
                log.error("     - 模板ID: {}, 名称: {}, 类型: {}, 状态: {}, 已部署: {}, ProcessKey: {}", 
                         template.getId(), template.getName(), template.getType(), 
                         template.getStatus(), template.getIsDeployed(), template.getProcessKey());
            }
            
            throw new RuntimeException("没有可用的工作流模板。请先在系统配置页面创建并部署工作流模板，然后重试。" +
                                     "当前系统中共有 " + allTemplates.size() + " 个工作流模板，其中 " + 
                                     allDeployedTemplates.size() + " 个已部署。");
            
        } catch (Exception e) {
            log.error("❌ 查找动态工作流模板失败: {}", e.getMessage(), e);
            throw new RuntimeException("工作流模板查找失败: " + e.getMessage());
        }
    }
    

    

    
    private Long getManagerId(UserDTO user) {
        // 获取直属主管ID
        if (user.getManagerId() != null) {
            // 验证主管是否存在
            Optional<UserDTO> manager = userService.getUserById(user.getManagerId());
            if (manager.isPresent() && "在职".equals(manager.get().getStatus())) {
                log.info("使用用户 {} 的直属主管: {} (ID: {})", user.getUserName(), 
                    manager.get().getUserName(), user.getManagerId());
                return user.getManagerId();
        }
        }
        
        // 如果没有直属主管或主管不存在，查找部门主管
        try {
            List<UserDTO> departmentManagers = userService.getUsersByDepartmentAndType(
                user.getDepartment(), "主管");
            
            if (!departmentManagers.isEmpty()) {
                UserDTO deptManager = departmentManagers.get(0);
                log.info("使用部门 {} 的主管: {} (ID: {})", user.getDepartment(), 
                    deptManager.getUserName(), deptManager.getId());
                return deptManager.getId();
            }
        } catch (Exception e) {
            log.warn("查找部门主管失败: {}", e.getMessage());
        }
        
        // 如果还是找不到，查找系统管理员
        Optional<UserDTO> admin = userService.getUserByEmail("admin@hkex.com");
        if (admin.isPresent()) {
            log.info("使用系统管理员作为默认审批人: {} (ID: {})", 
                admin.get().getUserName(), admin.get().getId());
            return admin.get().getId();
        }
        
        throw new RuntimeException("无法找到合适的审批人");
    }
    
    private Long getFunctionalHeadId(UserDTO user) {
        // 根据部门查找对应的职能负责人
        String functionalHead = null;
        
        switch (user.getDepartment()) {
            case "信息技术部":
                functionalHead = "CTO";
                break;
            case "财务部":
                functionalHead = "CFO";
                break;
            case "人力资源部":
                functionalHead = "COO";
                break;
            case "交易部":
                functionalHead = "CEO";
                break;
            case "风控部":
                functionalHead = "CRO";
                break;
            case "合规部":
                functionalHead = "CCO";
                break;
            default:
                functionalHead = "COO"; // 默认由COO审批
        }
        
        // 查找对应职位的用户
        try {
            List<UserDTO> functionalHeads = userService.getUsersByPosition(functionalHead);
            if (!functionalHeads.isEmpty()) {
                UserDTO head = functionalHeads.stream()
                    .filter(u -> "在职".equals(u.getStatus()))
                    .findFirst()
                    .orElse(null);
                    
                if (head != null) {
                    log.info("部门 {} 的职能负责人: {} - {} (ID: {})", 
                        user.getDepartment(), functionalHead, head.getUserName(), head.getId());
                    return head.getId();
                }
            }
        } catch (Exception e) {
            log.warn("查找职能负责人失败: {}", e.getMessage());
        }
        
        // 如果找不到对应的职能负责人，使用COO
        Optional<UserDTO> coo = userService.getUsersByPosition("COO").stream()
            .filter(u -> "在职".equals(u.getStatus()))
            .findFirst();
            
        if (coo.isPresent()) {
            log.info("使用COO作为默认职能负责人: {} (ID: {})", 
                coo.get().getUserName(), coo.get().getId());
            return coo.get().getId();
        }
        
        // 最后使用系统管理员
        Optional<UserDTO> admin = userService.getUserByEmail("admin@hkex.com");
        if (admin.isPresent()) {
            log.info("使用系统管理员作为默认职能负责人: {} (ID: {})", 
                admin.get().getUserName(), admin.get().getId());
            return admin.get().getId();
        }
        
        throw new RuntimeException("无法找到合适的职能负责人");
    }
    
    private Long getFinanceManagerId() {
        // 查找财务部主管
        List<UserDTO> financeManagers = userService.getUsersByDepartmentAndType("财务部", "主管");
        if (!financeManagers.isEmpty()) {
            UserDTO financeManager = financeManagers.get(0);
            log.info("使用财务部主管: {} (ID: {})", financeManager.getUserName(), financeManager.getId());
            return financeManager.getId();
        }
        
        // 如果找不到，使用张三（财务总监）
        Optional<UserDTO> zhangsan = userService.getUserByEmail("zhangsan@hkex.com");
        if (zhangsan.isPresent() && "在职".equals(zhangsan.get().getStatus())) {
            log.info("使用财务总监张三作为财务审批人");
            return zhangsan.get().getId();
        }
        
        throw new RuntimeException("无法找到财务部审批人");
    }
    
    private Long getComplianceManagerId() {
        // 查找合规部主管
        List<UserDTO> complianceManagers = userService.getUsersByDepartmentAndType("合规部", "主管");
        if (!complianceManagers.isEmpty()) {
            UserDTO complianceManager = complianceManagers.get(0);
            log.info("使用合规部主管: {} (ID: {})", complianceManager.getUserName(), complianceManager.getId());
            return complianceManager.getId();
        }
        
        // 如果找不到，使用严三（合规总监）
        Optional<UserDTO> yansan = userService.getUserByEmail("yansan@hkex.com");
        if (yansan.isPresent() && "在职".equals(yansan.get().getStatus())) {
            log.info("使用合规总监严三作为合规审批人");
            return yansan.get().getId();
        }
        
        throw new RuntimeException("无法找到合规部审批人");
    }
    
    private Long getExecutiveId(BigDecimal amount) {
        // 根据金额选择不同的高管审批
        String executivePosition;
        if (amount.compareTo(new BigDecimal("100000")) > 0) {
            executivePosition = "CEO";
        } else {
            executivePosition = "COO";
        }
        
        List<UserDTO> executives = userService.getUsersByPosition(executivePosition);
        if (!executives.isEmpty()) {
            UserDTO executive = executives.get(0);
            log.info("使用{}作为高管审批人: {} (ID: {})", executivePosition, executive.getUserName(), executive.getId());
            return executive.getId();
        }
        
        // 如果找不到对应高管，尝试查找其他高管
        String[] executivePositions = {"CEO", "COO", "CFO", "CTO"};
        for (String pos : executivePositions) {
            List<UserDTO> altExecutives = userService.getUsersByPosition(pos);
            if (!altExecutives.isEmpty()) {
                UserDTO executive = altExecutives.get(0);
                log.info("使用{}作为替代高管审批人: {} (ID: {})", pos, executive.getUserName(), executive.getId());
                return executive.getId();
            }
        }
        
        // 最后使用系统管理员
        Optional<UserDTO> admin = userService.getUserByEmail("admin@hkex.com");
        if (admin.isPresent()) {
            log.info("使用系统管理员作为高管审批人");
            return admin.get().getId();
        }
        
        throw new RuntimeException("无法找到高管审批人");
    }
    
    private void createWorkflowNode(WorkflowInstance instance, Task task) {
        WorkflowNode node = new WorkflowNode();
        node.setInstanceId(instance.getId());
        node.setTaskId(task.getId());
        node.setNodeKey(task.getTaskDefinitionKey());
        node.setNodeName(task.getName());
        node.setStatus("PENDING");
        node.setExecutionId(task.getExecutionId());
        
        if (task.getAssignee() != null) {
            try {
                node.setAssigneeId(Long.valueOf(task.getAssignee()));
                UserDTO assignee = userService.getUserById(Long.valueOf(task.getAssignee())).orElse(null);
                if (assignee != null) {
                    node.setAssigneeName(assignee.getUserName());
                }
            } catch (NumberFormatException e) {
                log.warn("无法解析任务分配人ID: {}", task.getAssignee());
            }
        }
        
        workflowNodeRepository.save(node);
    }
    
    private void updateWorkflowInstanceStatus(String processInstanceId) {
        WorkflowInstance instance = workflowInstanceRepository
            .findByProcessInstanceId(processInstanceId)
            .orElse(null);
        
        if (instance == null) {
            return;
        }
        
        // 检查流程是否结束
        HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
            .processInstanceId(processInstanceId)
            .singleResult();
        
        if (historicInstance != null && historicInstance.getEndTime() != null) {
            instance.setStatus("COMPLETED");
            instance.setEndTime(LocalDateTime.now());
        } else {
            // 获取当前任务
            Task currentTask = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
            
            if (currentTask != null) {
                instance.setCurrentNodeName(currentTask.getName());
                instance.setCurrentAssignee(currentTask.getAssignee());
                
                // 创建新的节点记录
                WorkflowNode existingNode = workflowNodeRepository
                    .findByTaskId(currentTask.getId())
                    .orElse(null);
                
                if (existingNode == null) {
                    createWorkflowNode(instance, currentTask);
                }
            }
        }
        
        workflowInstanceRepository.save(instance);
    }
    
    private WorkflowInstanceDTO convertToDTO(WorkflowInstance instance) {
        return WorkflowInstanceDTO.builder()
            .id(instance.getId())
            .processInstanceId(instance.getProcessInstanceId())
            .businessType(instance.getBusinessType())
            .businessId(instance.getBusinessId())
            .title(instance.getTitle())
            .status(instance.getStatus())
            .statusText(getStatusText(instance.getStatus()))
            .applicantId(instance.getApplicantId())
            .applicantName(instance.getApplicantName())
            .startTime(instance.getStartTime())
            .endTime(instance.getEndTime())
            .currentNodeName(instance.getCurrentNodeName())
            .currentAssignee(instance.getCurrentAssignee())
            .build();
    }
    
    private String getStatusText(String status) {
        switch (status) {
            case "RUNNING":
                return "进行中";
            case "COMPLETED":
                return "已完成";
            case "REJECTED":
                return "已拒绝";
            case "SUSPENDED":
                return "已暂停";
            case "TERMINATED":
                return "已终止";
            default:
                return status;
        }
    }
    
    /**
     * 映射WorkflowNode状态到前端期望的任务状态
     */
    private String mapNodeStatusToTaskStatus(String nodeStatus) {
        if (nodeStatus == null) {
            return "pending";
        }
        
        switch (nodeStatus.toUpperCase()) {
            case "PENDING":
            case "ASSIGNED":
                return "pending";
            case "IN_PROGRESS":
            case "ACTIVE":
                return "in-progress";
            case "COMPLETED":
                return "approved";
            case "REJECTED":
                return "rejected";
            case "RETURNED":
                return "pending";
            default:
                return "pending";
        }
    }
    
    /**
     * 根据用户ID获取用户名
     */
    private String getUserNameById(String userId) {
        try {
            if (userId == null) {
                return "未分配";
            }
            
            Long userIdLong = Long.valueOf(userId);
            Optional<UserDTO> user = userService.getUserById(userIdLong);
            return user.map(UserDTO::getUserName).orElse("未知用户");
        } catch (NumberFormatException e) {
            log.warn("无法解析用户ID: {}", userId);
            return "无效用户";
        } catch (Exception e) {
            log.warn("查询用户信息失败，用户ID: {}", userId, e);
            return "查询失败";
        }
    }
}
