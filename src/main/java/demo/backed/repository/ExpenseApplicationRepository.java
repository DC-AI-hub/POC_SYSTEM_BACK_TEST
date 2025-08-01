package demo.backed.repository;

import demo.backed.entity.ApplicationStatus;
import demo.backed.entity.ExpenseApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 费用申请Repository
 */
@Repository
public interface ExpenseApplicationRepository extends JpaRepository<ExpenseApplication, Long>, JpaSpecificationExecutor<ExpenseApplication> {
    
    /**
     * 根据申请人查询
     */
    Page<ExpenseApplication> findByApplicantId(Long applicantId, Pageable pageable);
    
    /**
     * 根据状态查询
     */
    Page<ExpenseApplication> findByStatus(ApplicationStatus status, Pageable pageable);
    
    /**
     * 根据申请人和状态查询
     */
    Page<ExpenseApplication> findByApplicantIdAndStatus(Long applicantId, ApplicationStatus status, Pageable pageable);
    
    /**
     * 根据部门查询
     */
    Page<ExpenseApplication> findByDepartment(String department, Pageable pageable);
    
    /**
     * 根据申请时间范围查询
     */
    @Query("SELECT e FROM ExpenseApplication e WHERE e.applyDate BETWEEN :startDate AND :endDate")
    Page<ExpenseApplication> findByApplyDateBetween(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable
    );
    
    /**
     * 根据申请时间范围和状态查询
     */
    @Query("SELECT e FROM ExpenseApplication e WHERE e.applyDate BETWEEN :startDate AND :endDate AND e.status = :status")
    Page<ExpenseApplication> findByApplyDateBetweenAndStatus(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("status") ApplicationStatus status,
        Pageable pageable
    );
    
    /**
     * 根据工作流实例ID查询
     */
    Optional<ExpenseApplication> findByWorkflowInstanceId(String workflowInstanceId);
    
    /**
     * 根据申请编号查询
     */
    Optional<ExpenseApplication> findByApplicationNumber(String applicationNumber);
    
    /**
     * 检查申请编号是否存在
     */
    boolean existsByApplicationNumber(String applicationNumber);
    
    /**
     * 根据状态统计数量
     */
    @Query("SELECT COUNT(e) FROM ExpenseApplication e WHERE e.status = :status")
    Long countByStatus(@Param("status") ApplicationStatus status);
    
    /**
     * 根据申请人和状态统计数量
     */
    Long countByApplicantIdAndStatus(Long applicantId, ApplicationStatus status);
    
    /**
     * 根据状态汇总金额
     */
    @Query("SELECT SUM(e.totalAmount) FROM ExpenseApplication e WHERE e.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") ApplicationStatus status);
    
    /**
     * 根据申请人和状态汇总金额
     */
    @Query("SELECT SUM(e.totalAmount) FROM ExpenseApplication e WHERE e.applicantId = :applicantId AND e.status = :status")
    BigDecimal sumAmountByApplicantIdAndStatus(@Param("applicantId") Long applicantId, @Param("status") ApplicationStatus status);
    
    /**
     * 根据部门和时间范围统计
     */
    @Query("SELECT COUNT(e) FROM ExpenseApplication e WHERE e.department = :department AND e.applyDate BETWEEN :startDate AND :endDate")
    Long countByDepartmentAndDateRange(
        @Param("department") String department,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    /**
     * 查询待审批的申请（提交状态或审批中状态）
     */
    @Query("SELECT e FROM ExpenseApplication e WHERE e.status IN ('SUBMITTED', 'IN_APPROVAL') ORDER BY e.submitTime ASC")
    Page<ExpenseApplication> findPendingApplications(Pageable pageable);
    
    /**
     * 根据申请人查询待审批的申请
     */
    @Query("SELECT e FROM ExpenseApplication e WHERE e.applicantId = :applicantId AND e.status IN ('SUBMITTED', 'IN_APPROVAL') ORDER BY e.submitTime ASC")
    Page<ExpenseApplication> findPendingApplicationsByApplicant(@Param("applicantId") Long applicantId, Pageable pageable);
    
    /**
     * 查询本月申请统计
     */
    @Query("SELECT COUNT(e), SUM(e.totalAmount) FROM ExpenseApplication e WHERE YEAR(e.applyDate) = :year AND MONTH(e.applyDate) = :month")
    Object[] getMonthlyStatistics(@Param("year") int year, @Param("month") int month);
} 