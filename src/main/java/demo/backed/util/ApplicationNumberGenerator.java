package demo.backed.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 申请编号生成器
 * 生成各种业务申请的唯一编号
 */
@Component
@Slf4j
public class ApplicationNumberGenerator {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private static final AtomicInteger EXPENSE_COUNTER = new AtomicInteger(0);
    
    // 编号前缀
    private static final String EXPENSE_PREFIX = "EXP";
    
    // 标记是否已初始化
    private volatile boolean expenseCounterInitialized = false;
    
    /**
     * 生成费用申请编号
     * 格式：EXP-YYYY-XXXXXX
     * 例如：EXP-2025-000001
     */
    public synchronized String generateExpenseNumber() {
        if (!expenseCounterInitialized) {
            initializeExpenseCounter();
        }
        return generateNumber(EXPENSE_PREFIX, EXPENSE_COUNTER);
    }
    

    
    /**
     * 初始化费用申请计数器
     */
    private void initializeExpenseCounter() {
        try {
            String currentYear = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
            String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(application_number, 10, 6) AS INTEGER)), 0) " +
                        "FROM t_poc_expense_applications " +
                        "WHERE application_number LIKE 'EXP-" + currentYear + "-%'";
            
            Integer maxSequence = jdbcTemplate.queryForObject(sql, Integer.class);
            EXPENSE_COUNTER.set(maxSequence != null ? maxSequence : 0);
            expenseCounterInitialized = true;
            
            log.info("费用申请计数器初始化完成，当前最大序列号: {}", maxSequence);
        } catch (Exception e) {
            log.error("初始化费用申请计数器失败", e);
            EXPENSE_COUNTER.set(0);
            expenseCounterInitialized = true;
        }
    }
    

    
    /**
     * 通用编号生成方法
     */
    private String generateNumber(String prefix, AtomicInteger counter) {
        LocalDate now = LocalDate.now();
        String year = now.format(DateTimeFormatter.ofPattern("yyyy"));
        
        // 获取当前计数器值并递增
        int sequence = counter.incrementAndGet();
        
        // 如果是新的一年，重置计数器
        resetCounterIfNewYear(counter, sequence);
        
        // 格式化序列号为6位数字
        String sequenceStr = String.format("%06d", sequence);
        
        return String.format("%s-%s-%s", prefix, year, sequenceStr);
    }
    
    /**
     * 如果是新的一年，重置计数器
     * 注意：这是一个简化的实现，生产环境中应该使用数据库或Redis来管理计数器
     */
    private void resetCounterIfNewYear(AtomicInteger counter, int currentValue) {
        // 这里的逻辑可以根据实际需求优化
        // 例如可以将上次重置的年份保存在配置中
        if (currentValue > 999999) { // 如果超过6位数，重置为1
            counter.set(1);
        }
    }
    

    

    

} 