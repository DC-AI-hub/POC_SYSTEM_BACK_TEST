package demo.backed.entity;

/**
 * 申请状态枚举
 * 用于费用申请和差旅申请等业务状态管理
 */
public enum ApplicationStatus {
    DRAFT("草稿"),
    SUBMITTED("已提交"),
    IN_APPROVAL("审批中"),
    APPROVED("已通过"),
    REJECTED("已拒绝"),
    RETURNED("已打回");
    
    private final String description;
    
    ApplicationStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 获取状态的中文描述
     */
    public String getDisplayName() {
        return description;
    }
    
    /**
     * 判断是否为终态（不可再变更）
     */
    public boolean isFinalStatus() {
        return this == APPROVED || this == REJECTED;
    }
    
    /**
     * 判断是否可以提交审批
     */
    public boolean canSubmitForApproval() {
        return this == DRAFT || this == RETURNED;
    }
    
    /**
     * 判断是否可以编辑
     */
    public boolean canEdit() {
        return this == DRAFT || this == RETURNED;
    }
} 