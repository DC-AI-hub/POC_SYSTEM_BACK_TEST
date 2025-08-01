package demo.backed.repository;

import demo.backed.entity.ExpenseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 费用明细Repository
 */
@Repository
public interface ExpenseItemRepository extends JpaRepository<ExpenseItem, Long> {
    
    /**
     * 根据申请单ID查询明细，按排序字段排序
     */
    List<ExpenseItem> findByApplicationIdOrderBySortOrder(Long applicationId);
    
    /**
     * 根据申请单ID查询明细
     */
    List<ExpenseItem> findByApplicationId(Long applicationId);
    
    /**
     * 根据申请单ID删除明细
     */
    @Modifying
    @Transactional
    void deleteByApplicationId(Long applicationId);
    
    /**
     * 根据申请单ID汇总金额
     */
    @Query("SELECT SUM(ei.amount) FROM ExpenseItem ei WHERE ei.applicationId = :applicationId")
    BigDecimal sumAmountByApplicationId(@Param("applicationId") Long applicationId);
    
    /**
     * 根据申请单ID统计明细数量
     */
    Long countByApplicationId(Long applicationId);
    
    /**
     * 根据费用科目查询明细
     */
    List<ExpenseItem> findByExpenseCategory(String expenseCategory);
    
    /**
     * 根据费用科目统计数量
     */
    Long countByExpenseCategory(String expenseCategory);
    
    /**
     * 根据费用科目汇总金额
     */
    @Query("SELECT SUM(ei.amount) FROM ExpenseItem ei WHERE ei.expenseCategory = :expenseCategory")
    BigDecimal sumAmountByExpenseCategory(@Param("expenseCategory") String expenseCategory);
    
    /**
     * 根据费用发生日期范围查询明细
     */
    @Query("SELECT ei FROM ExpenseItem ei WHERE ei.expenseDate BETWEEN :startDate AND :endDate")
    List<ExpenseItem> findByExpenseDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * 查询需要发票的明细
     */
    @Query("SELECT ei FROM ExpenseItem ei WHERE ei.receiptRequired = true AND ei.applicationId = :applicationId")
    List<ExpenseItem> findReceiptRequiredByApplicationId(@Param("applicationId") Long applicationId);
    
    /**
     * 统计需要发票的明细数量
     */
    @Query("SELECT COUNT(ei) FROM ExpenseItem ei WHERE ei.receiptRequired = true AND ei.applicationId = :applicationId")
    Long countReceiptRequiredByApplicationId(@Param("applicationId") Long applicationId);
    
    /**
     * 查询金额超过指定值的明细
     */
    @Query("SELECT ei FROM ExpenseItem ei WHERE ei.amount > :amount")
    List<ExpenseItem> findByAmountGreaterThan(@Param("amount") BigDecimal amount);
    
    /**
     * 根据申请单ID批量更新排序
     */
    @Modifying
    @Transactional
    @Query("UPDATE ExpenseItem ei SET ei.sortOrder = :sortOrder WHERE ei.id = :id")
    int updateSortOrder(@Param("id") Long id, @Param("sortOrder") Integer sortOrder);
    
    /**
     * 获取申请单的最大排序号
     */
    @Query("SELECT COALESCE(MAX(ei.sortOrder), 0) FROM ExpenseItem ei WHERE ei.applicationId = :applicationId")
    Integer findMaxSortOrderByApplicationId(@Param("applicationId") Long applicationId);
    
    /**
     * 根据申请单ID列表批量查询明细
     */
    @Query("SELECT ei FROM ExpenseItem ei WHERE ei.applicationId IN :applicationIds ORDER BY ei.applicationId, ei.sortOrder")
    List<ExpenseItem> findByApplicationIdIn(@Param("applicationIds") List<Long> applicationIds);
    
    /**
     * 费用科目分组统计
     */
    @Query("SELECT ei.expenseCategory, COUNT(ei), SUM(ei.amount) FROM ExpenseItem ei GROUP BY ei.expenseCategory")
    List<Object[]> getCategoryStatistics();
    
    /**
     * 按月份分组统计费用
     */
    @Query("SELECT YEAR(ei.expenseDate), MONTH(ei.expenseDate), SUM(ei.amount) FROM ExpenseItem ei WHERE ei.expenseDate BETWEEN :startDate AND :endDate GROUP BY YEAR(ei.expenseDate), MONTH(ei.expenseDate)")
    List<Object[]> getMonthlyExpenseStatistics(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
} 