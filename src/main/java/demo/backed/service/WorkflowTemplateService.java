package demo.backed.service;

import demo.backed.entity.WorkflowTemplate;
import demo.backed.repository.WorkflowTemplateRepository;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 工作流模板服务
 */
@Service
@Transactional
@Slf4j
public class WorkflowTemplateService {
    
    @Autowired
    private WorkflowTemplateRepository templateRepository;
    
    @Autowired
    private RepositoryService repositoryService;
    
    /**
     * 获取所有模板
     */
    public List<WorkflowTemplate> getAllTemplates() {
        return templateRepository.findAllActive();
    }
    
    /**
     * 根据ID获取模板
     */
    public WorkflowTemplate getTemplateById(Long id) {
        return templateRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("工作流模板不存在"));
    }
    
    /**
     * 创建工作流模板
     */
    public WorkflowTemplate createTemplate(WorkflowTemplate template) {
        // 生成唯一的流程Key
        if (template.getProcessKey() == null || template.getProcessKey().isEmpty()) {
            template.setProcessKey("process_" + UUID.randomUUID().toString().replace("-", ""));
        }
        
        // 检查流程Key是否已存在
        if (templateRepository.existsByProcessKey(template.getProcessKey())) {
            throw new RuntimeException("流程Key已存在: " + template.getProcessKey());
        }
        
        // 设置默认的BPMN XML
        if (template.getBpmnXml() == null || template.getBpmnXml().isEmpty()) {
            template.setBpmnXml(generateDefaultBpmnXml(template));
        }
        
        template.setStatus("draft");
        template.setIsDeployed(false);
        template.setTemplateVersion(1);
        
        WorkflowTemplate saved = templateRepository.save(template);
        log.info("创建工作流模板: {}", saved.getName());
        
        return saved;
    }
    
    /**
     * 更新工作流模板
     */
    public WorkflowTemplate updateTemplate(Long id, WorkflowTemplate templateUpdate) {
        WorkflowTemplate template = getTemplateById(id);
        
        // 更新基本信息
        template.setName(templateUpdate.getName());
        template.setDescription(templateUpdate.getDescription());
        template.setType(templateUpdate.getType());
        
        // 更新配置数据
        if (templateUpdate.getConfigData() != null) {
            template.setConfigData(templateUpdate.getConfigData());
            log.info("更新工作流配置数据，长度: {}", templateUpdate.getConfigData().length());
        }
        
        // 如果BPMN XML有变化，增加版本号
        if (templateUpdate.getBpmnXml() != null && 
            !templateUpdate.getBpmnXml().equals(template.getBpmnXml())) {
            template.setBpmnXml(templateUpdate.getBpmnXml());
            template.setTemplateVersion(template.getTemplateVersion() + 1);
            template.setIsDeployed(false); // 需要重新部署
        }
        
        WorkflowTemplate saved = templateRepository.save(template);
        log.info("更新工作流模板: {}", saved.getName());
        
        return saved;
    }
    
    /**
     * 删除工作流模板
     */
    public void deleteTemplate(Long id) {
        WorkflowTemplate template = getTemplateById(id);
        
        // 如果已部署，先取消部署
        if (template.getIsDeployed() && template.getDeploymentId() != null) {
            try {
                repositoryService.deleteDeployment(template.getDeploymentId(), true);
                log.info("取消部署: {}", template.getDeploymentId());
            } catch (Exception e) {
                log.error("取消部署失败", e);
            }
        }
        
        // 软删除
        template.softDelete();
        templateRepository.save(template);
        log.info("删除工作流模板: {}", template.getName());
    }
    
    /**
     * 部署工作流模板到Flowable
     */
    public WorkflowTemplate deployTemplate(Long id) {
        WorkflowTemplate template = getTemplateById(id);
        
        if (template.getBpmnXml() == null || template.getBpmnXml().isEmpty()) {
            throw new RuntimeException("BPMN XML内容为空，无法部署");
        }
        
        try {
            // 创建部署
            DeploymentBuilder deploymentBuilder = repositoryService.createDeployment()
                .name(template.getName())
                .category(template.getType())
                .key(template.getProcessKey());
            
            // 添加BPMN资源
            deploymentBuilder.addBytes(
                template.getProcessKey() + ".bpmn20.xml",
                template.getBpmnXml().getBytes(StandardCharsets.UTF_8)
            );
            
            // 执行部署
            Deployment deployment = deploymentBuilder.deploy();
            
            // 更新模板状态
            template.setDeploymentId(deployment.getId());
            template.setIsDeployed(true);
            template.setDeployedTime(LocalDateTime.now());
            template.setStatus("active");
            
            templateRepository.save(template);
            log.info("成功部署工作流模板: {} -> {}", template.getName(), deployment.getId());
            
            return template;
        } catch (Exception e) {
            log.error("部署工作流模板失败", e);
            throw new RuntimeException("部署失败: " + e.getMessage());
        }
    }
    
    /**
     * 取消部署工作流模板
     */
    public WorkflowTemplate undeployTemplate(Long id) {
        WorkflowTemplate template = getTemplateById(id);
        
        if (!template.getIsDeployed() || template.getDeploymentId() == null) {
            throw new RuntimeException("工作流模板未部署");
        }
        
        try {
            // 删除部署
            repositoryService.deleteDeployment(template.getDeploymentId(), true);
            
            // 更新模板状态
            template.setDeploymentId(null);
            template.setIsDeployed(false);
            template.setDeployedTime(null);
            template.setStatus("draft");
            
            templateRepository.save(template);
            log.info("成功取消部署工作流模板: {}", template.getName());
            
            return template;
        } catch (Exception e) {
            log.error("取消部署工作流模板失败", e);
            throw new RuntimeException("取消部署失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据业务类型查找已部署的工作流模板
     */
    public Optional<WorkflowTemplate> findDeployedTemplateByBusinessType(String businessType) {
        try {
            log.info("🔍 查找业务类型 {} 的已部署工作流模板", businessType);
            
            // 1. 查找所有已部署且激活的模板
            List<WorkflowTemplate> deployedTemplates = templateRepository.findByIsDeployedAndStatus(true, "active");
            log.info("📊 数据库中已部署且激活的工作流模板数量: {}", deployedTemplates.size());
            
            // 打印所有已部署的模板信息
            for (WorkflowTemplate template : deployedTemplates) {
                log.info("📋 已部署模板: {} (类型: {}, processKey: {}, 部署ID: {})", 
                        template.getName(), template.getType(), template.getProcessKey(), template.getDeploymentId());
            }
            
            // 2. 精确匹配业务类型
            Optional<WorkflowTemplate> exactMatch = deployedTemplates.stream()
                .filter(template -> isExactBusinessTypeMatch(template, businessType))
                .findFirst();
            
            if (exactMatch.isPresent()) {
                log.info("✅ 找到精确匹配的工作流模板: {} (processKey: {})", 
                        exactMatch.get().getName(), exactMatch.get().getProcessKey());
                return exactMatch;
            }
            
            // 3. 模糊匹配业务类型
            Optional<WorkflowTemplate> fuzzyMatch = deployedTemplates.stream()
                .filter(template -> isFuzzyBusinessTypeMatch(template, businessType))
                .findFirst();
            
            if (fuzzyMatch.isPresent()) {
                log.info("✅ 找到模糊匹配的工作流模板: {} (processKey: {})", 
                        fuzzyMatch.get().getName(), fuzzyMatch.get().getProcessKey());
                return fuzzyMatch;
            }
            
            // 4. 如果没有找到匹配的，尝试查找所有已部署的模板
            if (!deployedTemplates.isEmpty()) {
                log.warn("⚠️ 未找到匹配业务类型 {} 的工作流模板，但数据库中有 {} 个已部署的模板", 
                        businessType, deployedTemplates.size());
                
                // 作为fallback，返回第一个已部署的模板
                WorkflowTemplate fallbackTemplate = deployedTemplates.get(0);
                log.info("🔄 使用第一个可用的工作流模板作为fallback: {} (processKey: {})", 
                        fallbackTemplate.getName(), fallbackTemplate.getProcessKey());
                return Optional.of(fallbackTemplate);
            }
            
            log.error("❌ 数据库中没有任何已部署且激活的工作流模板！请检查系统配置页面的工作流部署状态。");
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("❌ 查找工作流模板失败", e);
            return Optional.empty();
        }
    }
    
    /**
     * 精确匹配业务类型
     */
    private boolean isExactBusinessTypeMatch(WorkflowTemplate template, String businessType) {
        if (template.getType() != null && template.getType().equalsIgnoreCase(businessType)) {
            log.info("✅ 工作流模板类型匹配: {} = {}", template.getType(), businessType);
            return true;
        }
        
        String nameLower = template.getName().toLowerCase();
        String businessTypeLower = businessType.toLowerCase();
        
        // 精确关键词匹配
        if (businessTypeLower.contains("expense") || businessTypeLower.contains("费用")) {
            boolean matches = nameLower.contains("费用") || nameLower.contains("expense") || "expense".equals(template.getType());
            if (matches) {
                log.info("✅ 工作流模板名称匹配费用类型: {} -> {}", template.getName(), businessType);
            }
            return matches;
        }
        
        if (businessTypeLower.contains("travel") || businessTypeLower.contains("差旅")) {
            boolean matches = nameLower.contains("差旅") || nameLower.contains("travel") || "travel".equals(template.getType());
            if (matches) {
                log.info("✅ 工作流模板名称匹配差旅类型: {} -> {}", template.getName(), businessType);
            }
            return matches;
        }
        
        log.debug("❌ 工作流模板不匹配: {} (type: {}) vs {}", template.getName(), template.getType(), businessType);
        return false;
    }
    
    /**
     * 模糊匹配业务类型
     */
    private boolean isFuzzyBusinessTypeMatch(WorkflowTemplate template, String businessType) {
        String nameLower = template.getName().toLowerCase();
        String descLower = template.getDescription() != null ? template.getDescription().toLowerCase() : "";
        String businessTypeLower = businessType.toLowerCase();
        
        // 检查名称和描述中是否包含相关关键词
        String[] keywords = businessTypeLower.split("_");
        for (String keyword : keywords) {
            if (keyword.length() > 2 && (nameLower.contains(keyword) || descLower.contains(keyword))) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取默认的费用审批工作流模板
     */
    public Optional<WorkflowTemplate> getDefaultExpenseTemplate() {
        return templateRepository.findByIsDeployedAndStatus(true, "active").stream()
            .filter(template -> "expense".equals(template.getType()))
            .findFirst();
    }
    
    /**
     * 验证工作流模板是否可用
     */
    public boolean isTemplateAvailable(String processKey) {
        try {
            // 首先检查实际的processKey
            boolean available = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processKey)
                .active()
                .count() > 0;
            
            if (available) {
                log.info("✅ 找到匹配的流程定义: {}", processKey);
                return true;
            }
            
            // 兼容性处理：检查是否使用了固定的expenseApproval key部署
            if (!processKey.equals("expenseApproval")) {
                boolean fallbackAvailable = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey("expenseApproval")
                    .active()
                    .count() > 0;
                
                if (fallbackAvailable) {
                    log.warn("⚠️ 未找到processKey {}，但找到了expenseApproval，使用兼容模式", processKey);
                    return true;
                }
            }
            
            log.error("❌ 未找到可用的流程定义: {}", processKey);
            return false;
        } catch (Exception e) {
            log.error("验证工作流模板可用性失败", e);
            return false;
        }
    }
    
    /**
     * 生成默认的BPMN XML
     */
    private String generateDefaultBpmnXml(WorkflowTemplate template) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
            "             xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "             xmlns:flowable=\"http://flowable.org/bpmn\"\n" +
            "             xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\"\n" +
            "             xmlns:omgdc=\"http://www.omg.org/spec/DD/20100524/DC\"\n" +
            "             xmlns:omgdi=\"http://www.omg.org/spec/DD/20100524/DI\"\n" +
            "             targetNamespace=\"http://hkex.com/poc/workflow\">\n" +
            "             \n" +
            "  <process id=\"" + template.getProcessKey() + "\" name=\"" + template.getName() + "\" isExecutable=\"true\">\n" +
            "    \n" +
            "    <startEvent id=\"start\" name=\"开始\"/>\n" +
            "    \n" +
            "    <userTask id=\"task1\" name=\"任务1\" flowable:assignee=\"${initiator}\"/>\n" +
            "    \n" +
            "    <sequenceFlow id=\"flow1\" sourceRef=\"start\" targetRef=\"task1\"/>\n" +
            "    \n" +
            "    <endEvent id=\"end\" name=\"结束\"/>\n" +
            "    \n" +
            "    <sequenceFlow id=\"flow2\" sourceRef=\"task1\" targetRef=\"end\"/>\n" +
            "    \n" +
            "  </process>\n" +
            "  \n" +
            "  <!-- BPMN图形定义 -->\n" +
            "  <bpmndi:BPMNDiagram id=\"BPMNDiagram_" + template.getProcessKey() + "\">\n" +
            "    <bpmndi:BPMNPlane id=\"BPMNPlane_" + template.getProcessKey() + "\" bpmnElement=\"" + template.getProcessKey() + "\">\n" +
            "      <bpmndi:BPMNShape id=\"start_di\" bpmnElement=\"start\">\n" +
            "        <omgdc:Bounds x=\"100\" y=\"180\" width=\"36\" height=\"36\" />\n" +
            "        <bpmndi:BPMNLabel>\n" +
            "          <omgdc:Bounds x=\"102\" y=\"223\" width=\"33\" height=\"14\" />\n" +
            "        </bpmndi:BPMNLabel>\n" +
            "      </bpmndi:BPMNShape>\n" +
            "      \n" +
            "      <bpmndi:BPMNShape id=\"task1_di\" bpmnElement=\"task1\">\n" +
            "        <omgdc:Bounds x=\"200\" y=\"158\" width=\"100\" height=\"80\" />\n" +
            "      </bpmndi:BPMNShape>\n" +
            "      \n" +
            "      <bpmndi:BPMNShape id=\"end_di\" bpmnElement=\"end\">\n" +
            "        <omgdc:Bounds x=\"370\" y=\"180\" width=\"36\" height=\"36\" />\n" +
            "        <bpmndi:BPMNLabel>\n" +
            "          <omgdc:Bounds x=\"372\" y=\"223\" width=\"33\" height=\"14\" />\n" +
            "        </bpmndi:BPMNLabel>\n" +
            "      </bpmndi:BPMNShape>\n" +
            "      \n" +
            "      <bpmndi:BPMNEdge id=\"flow1_di\" bpmnElement=\"flow1\">\n" +
            "        <omgdi:waypoint x=\"136\" y=\"198\" />\n" +
            "        <omgdi:waypoint x=\"200\" y=\"198\" />\n" +
            "      </bpmndi:BPMNEdge>\n" +
            "      \n" +
            "      <bpmndi:BPMNEdge id=\"flow2_di\" bpmnElement=\"flow2\">\n" +
            "        <omgdi:waypoint x=\"300\" y=\"198\" />\n" +
            "        <omgdi:waypoint x=\"370\" y=\"198\" />\n" +
            "      </bpmndi:BPMNEdge>\n" +
            "    </bpmndi:BPMNPlane>\n" +
            "  </bpmndi:BPMNDiagram>\n" +
            "  \n" +
            "</definitions>";
    }
} 