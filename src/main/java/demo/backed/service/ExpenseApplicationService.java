package demo.backed.service;

import demo.backed.dto.*;
import demo.backed.entity.ApplicationStatus;
import demo.backed.entity.ExpenseApplication;
import demo.backed.entity.ExpenseItem;
import demo.backed.repository.ExpenseApplicationRepository;
import demo.backed.repository.ExpenseItemRepository;
import demo.backed.util.ApplicationNumberGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * 费用申请业务逻辑服务
 */
@Service
@Transactional
@Slf4j
public class ExpenseApplicationService {
    
    @Autowired
    private ExpenseApplicationRepository applicationRepository;
    
    @Autowired
    private ExpenseItemRepository itemRepository;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ApplicationNumberGenerator numberGenerator;
    
    @Autowired
    private WorkflowIntegrationService workflowIntegrationService;
    
    /**
     * 创建费用申请
     */
    public ExpenseApplicationDTO createApplication(CreateExpenseApplicationDTO dto) {
        log.info("开始创建费用申请，申请人ID: {}", dto.getApplicantId());
        
        // 1. 验证请求数据
        dto.validate();
        
        // 2. 验证申请人信息
        UserDTO applicant = userService.getUserById(dto.getApplicantId())
            .orElseThrow(() -> new RuntimeException("申请人不存在，ID: " + dto.getApplicantId()));
        
        // 3. 创建申请单实体
        ExpenseApplication application = new ExpenseApplication();
        application.setApplicationNumber(numberGenerator.generateExpenseNumber());
        application.setApplicantId(dto.getApplicantId());
        application.setApplicantName(applicant.getUserName());
        application.setDepartment(applicant.getDepartment());
        application.setCompany(dto.getCompany());
        application.setApplyDate(dto.getApplyDate());
        application.setDescription(dto.getDescription());
        application.setCurrency(dto.getCurrency());
        application.setStatus(ApplicationStatus.DRAFT);
        
        // 4. 计算总金额
        BigDecimal totalAmount = dto.getItems().stream()
                .map(ExpenseItemDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        application.setTotalAmount(totalAmount);
        
        // 5. 保存申请单
        ExpenseApplication savedApplication = applicationRepository.save(application);
        
        // 6. 保存费用明细
        List<ExpenseItem> items = new ArrayList<>();
        for (int i = 0; i < dto.getItems().size(); i++) {
            ExpenseItemDTO itemDto = dto.getItems().get(i);
            ExpenseItem item = convertToItemEntity(itemDto);
            item.setApplicationId(savedApplication.getId());
            item.setSortOrder(i);
            items.add(item);
        }
        itemRepository.saveAll(items);
        
        log.info("费用申请创建成功，申请编号: {}, 总金额: {}", 
                savedApplication.getApplicationNumber(), totalAmount);
        
        return convertToDTO(savedApplication, items);
    }
    
    /**
     * 提交审批
     */
    public ExpenseApplicationDTO submitForApproval(Long applicationId) {
        log.info("提交费用申请审批，申请ID: {}", applicationId);
        
        ExpenseApplication application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new RuntimeException("申请单不存在，ID: " + applicationId));
        
        // 验证状态
        if (!application.canSubmitForApproval()) {
            throw new RuntimeException("当前状态不允许提交审批: " + application.getStatus().getDescription());
        }
        
        // 验证必要信息
        application.validateForSubmission();
        
        // 更新状态
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setSubmitTime(LocalDateTime.now());
        
        // 启动工作流
        String workflowInstanceId = workflowIntegrationService.startExpenseApprovalWorkflow(application);
        application.setWorkflowInstanceId(workflowInstanceId);
        
        ExpenseApplication savedApplication = applicationRepository.save(application);
        List<ExpenseItem> items = itemRepository.findByApplicationIdOrderBySortOrder(applicationId);
        
        log.info("费用申请提交成功，申请编号: {}", savedApplication.getApplicationNumber());
        
        return convertToDTO(savedApplication, items);
    }
    
    /**
     * 更新申请状态（由工作流回调）
     */
    public void updateApplicationStatus(String workflowInstanceId, ApplicationStatus status, String comment) {
        log.info("更新申请状态，工作流实例ID: {}, 新状态: {}", workflowInstanceId, status);
        
        ExpenseApplication application = applicationRepository.findByWorkflowInstanceId(workflowInstanceId)
            .orElseThrow(() -> new RuntimeException("找不到对应的申请单，工作流实例ID: " + workflowInstanceId));
        
        ApplicationStatus oldStatus = application.getStatus();
        application.setStatus(status);
        
        applicationRepository.save(application);
        
        log.info("申请状态更新成功，申请编号: {}, {} -> {}", 
                application.getApplicationNumber(), oldStatus, status);
    }
    
    /**
     * 分页查询费用申请
     */
    @Transactional(readOnly = true)
    public Page<ExpenseApplicationDTO> findApplications(ExpenseQueryDTO queryDto, Pageable pageable) {
        log.debug("查询费用申请列表，查询条件: {}", queryDto);
        
        Specification<ExpenseApplication> spec = buildSpecification(queryDto);
        Page<ExpenseApplication> applications = applicationRepository.findAll(spec, pageable);
        
        return applications.map(app -> {
            List<ExpenseItem> items = itemRepository.findByApplicationIdOrderBySortOrder(app.getId());
            return convertToDTO(app, items);
        });
    }
    
    /**
     * 获取申请单详情
     */
    @Transactional(readOnly = true)
    public ExpenseApplicationDTO getApplicationDetail(Long id) {
        log.debug("获取费用申请详情，ID: {}", id);
        
        ExpenseApplication application = applicationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("申请单不存在，ID: " + id));
        
        List<ExpenseItem> items = itemRepository.findByApplicationIdOrderBySortOrder(id);
        
        return convertToDTO(application, items);
    }
    
    /**
     * 更新费用申请
     */
    public ExpenseApplicationDTO updateApplication(Long id, CreateExpenseApplicationDTO dto) {
        log.info("更新费用申请，ID: {}", id);
        
        ExpenseApplication application = applicationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("申请单不存在，ID: " + id));
        
        // 验证状态
        if (!application.canEdit()) {
            throw new RuntimeException("当前状态不允许编辑: " + application.getStatus().getDescription());
        }
        
        // 验证请求数据
        dto.validate();
        
        // 更新申请单信息
        application.setCompany(dto.getCompany());
        application.setApplyDate(dto.getApplyDate());
        application.setDescription(dto.getDescription());
        application.setCurrency(dto.getCurrency());
        
        // 计算新的总金额
        BigDecimal totalAmount = dto.getItems().stream()
                .map(ExpenseItemDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        application.setTotalAmount(totalAmount);
        
        // 删除原有明细
        itemRepository.deleteByApplicationId(id);
        
        // 保存新明细
        List<ExpenseItem> items = new ArrayList<>();
        for (int i = 0; i < dto.getItems().size(); i++) {
            ExpenseItemDTO itemDto = dto.getItems().get(i);
            ExpenseItem item = convertToItemEntity(itemDto);
            item.setApplicationId(id);
            item.setSortOrder(i);
            items.add(item);
        }
        itemRepository.saveAll(items);
        
        ExpenseApplication savedApplication = applicationRepository.save(application);
        
        log.info("费用申请更新成功，申请编号: {}", savedApplication.getApplicationNumber());
        
        return convertToDTO(savedApplication, items);
    }
    
    /**
     * 删除费用申请
     */
    public void deleteApplication(Long id) {
        log.info("删除费用申请，ID: {}", id);
        
        ExpenseApplication application = applicationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("申请单不存在，ID: " + id));
        
        // 验证状态
        if (!application.canEdit()) {
            throw new RuntimeException("当前状态不允许删除: " + application.getStatus().getDescription());
        }
        
        // 删除明细
        itemRepository.deleteByApplicationId(id);
        
        // 删除申请单
        applicationRepository.deleteById(id);
        
        log.info("费用申请删除成功，申请编号: {}", application.getApplicationNumber());
    }
    
    /**
     * 构建查询条件
     */
    private Specification<ExpenseApplication> buildSpecification(ExpenseQueryDTO queryDto) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (queryDto.getApplicantId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("applicantId"), queryDto.getApplicantId()));
            }
            
            if (queryDto.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), queryDto.getStatus()));
            }
            
            if (StringUtils.hasText(queryDto.getDepartment())) {
                predicates.add(criteriaBuilder.like(root.get("department"), "%" + queryDto.getDepartment() + "%"));
            }
            
            if (StringUtils.hasText(queryDto.getApplicationNumber())) {
                predicates.add(criteriaBuilder.like(root.get("applicationNumber"), "%" + queryDto.getApplicationNumber() + "%"));
            }
            
            if (queryDto.getStartDate() != null && queryDto.getEndDate() != null) {
                predicates.add(criteriaBuilder.between(root.get("applyDate"), queryDto.getStartDate(), queryDto.getEndDate()));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * 转换实体为DTO
     */
    private ExpenseApplicationDTO convertToDTO(ExpenseApplication application, List<ExpenseItem> items) {
        ExpenseApplicationDTO dto = new ExpenseApplicationDTO();
        dto.setId(application.getId());
        dto.setApplicationNumber(application.getApplicationNumber());
        dto.setApplicantId(application.getApplicantId());
        dto.setApplicantName(application.getApplicantName());
        dto.setDepartment(application.getDepartment());
        dto.setCompany(application.getCompany());
        dto.setApplyDate(application.getApplyDate());
        dto.setDescription(application.getDescription());
        dto.setTotalAmount(application.getTotalAmount());
        dto.setCurrency(application.getCurrency());
        dto.setStatus(application.getStatus());
        dto.setSubmitTime(application.getSubmitTime());
        dto.setWorkflowInstanceId(application.getWorkflowInstanceId());
        dto.setCreatedTime(application.getCreatedTime());
        dto.setUpdatedTime(application.getUpdatedTime());
        dto.setCreatedBy(application.getCreatedBy());
        dto.setUpdatedBy(application.getUpdatedBy());
        
        // 转换明细
        List<ExpenseItemDTO> itemDTOs = new ArrayList<>();
        for (ExpenseItem item : items) {
            itemDTOs.add(convertToItemDTO(item));
        }
        dto.setItems(itemDTOs);
        
        return dto;
    }
    
    /**
     * 转换DTO为明细实体
     */
    private ExpenseItem convertToItemEntity(ExpenseItemDTO dto) {
        ExpenseItem item = new ExpenseItem();
        item.setExpenseCategory(dto.getExpenseCategory());
        item.setPurpose(dto.getPurpose());
        item.setAmount(dto.getAmount());
        item.setExpenseDate(dto.getExpenseDate());
        item.setRemark(dto.getRemark());
        item.setReceiptRequired(dto.getReceiptRequired());
        return item;
    }
    
    /**
     * 转换明细实体为DTO
     */
    private ExpenseItemDTO convertToItemDTO(ExpenseItem item) {
        ExpenseItemDTO dto = new ExpenseItemDTO();
        dto.setId(item.getId());
        dto.setApplicationId(item.getApplicationId());
        dto.setExpenseCategory(item.getExpenseCategory());
        dto.setPurpose(item.getPurpose());
        dto.setAmount(item.getAmount());
        dto.setExpenseDate(item.getExpenseDate());
        dto.setRemark(item.getRemark());
        dto.setSortOrder(item.getSortOrder());
        dto.setReceiptRequired(item.getReceiptRequired());
        return dto;
    }
} 