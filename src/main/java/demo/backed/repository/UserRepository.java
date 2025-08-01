package demo.backed.repository;

import demo.backed.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据工号查找用户
     */
    Optional<User> findByEmployeeId(String employeeId);
    
    /**
     * 根据邮箱查找用户 - 核心方法：邮箱作为唯一键
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 根据Keycloak ID查找用户
     */
    Optional<User> findByKeycloakId(String keycloakId);
    
    /**
     * 获取在线用户列表
     */
    List<User> findByIsOnlineTrue();
    
    /**
     * 根据部门查找用户
     */
    List<User> findByDepartment(String department);
    
    /**
     * 根据状态查找用户
     */
    List<User> findByStatus(String status);
    
    /**
     * 复合条件查询
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:keyword IS NULL OR u.userName LIKE %:keyword% OR u.employeeId LIKE %:keyword% OR u.email LIKE %:keyword%) AND " +
           "(:department IS NULL OR u.department = :department) AND " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:userType IS NULL OR u.userType = :userType)")
    Page<User> findByConditions(@Param("keyword") String keyword,
                               @Param("department") String department,
                               @Param("status") String status,
                               @Param("userType") String userType,
                               Pageable pageable);
    
    /**
     * 检查工号是否存在（排除指定ID）
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.employeeId = :employeeId AND (:excludeId IS NULL OR u.id != :excludeId)")
    long countByEmployeeIdAndIdNot(@Param("employeeId") String employeeId, @Param("excludeId") Long excludeId);
    
    /**
     * 检查邮箱是否存在（排除指定ID）- 核心方法：邮箱唯一性验证
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.email = :email AND (:excludeId IS NULL OR u.id != :excludeId)")
    long countByEmailAndIdNot(@Param("email") String email, @Param("excludeId") Long excludeId);
    
    /**
     * 统计在线用户数量
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isOnline = true")
    long countOnlineUsers();
    
    /**
     * 按部门统计用户数量
     */
    @Query("SELECT u.department, COUNT(u) FROM User u WHERE u.status = '在职' GROUP BY u.department")
    List<Object[]> countUsersByDepartment();
    
    /**
     * 邮箱唯一性验证方法
     */
    boolean existsByEmail(String email);
    
    boolean existsByEmployeeId(String employeeId);
    
    /**
     * 查询所有不同的部门
     */
    @Query("SELECT DISTINCT u.department FROM User u WHERE u.department IS NOT NULL AND u.department != '' ORDER BY u.department")
    List<String> findDistinctDepartments();
    
    /**
     * 根据部门和用户类型查询
     */
    List<User> findByDepartmentAndUserType(String department, String userType);
    
    /**
     * 根据职位查询
     */
    List<User> findByPosition(String position);
    
    // ==================== 新增方法：强化邮箱唯一性支持 ====================
    
    /**
     * 根据邮箱列表批量查找用户
     */
    @Query("SELECT u FROM User u WHERE u.email IN :emails")
    List<User> findByEmailIn(@Param("emails") List<String> emails);
    
    /**
     * 查找有Keycloak ID的用户
     */
    @Query("SELECT u FROM User u WHERE u.keycloakId IS NOT NULL")
    List<User> findUsersWithKeycloakId();
    
    /**
     * 查找没有Keycloak ID的用户
     */
    @Query("SELECT u FROM User u WHERE u.keycloakId IS NULL")
    List<User> findUsersWithoutKeycloakId();
    
    /**
     * 根据邮箱和状态查找用户
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.status = :status")
    Optional<User> findByEmailAndStatus(@Param("email") String email, @Param("status") String status);
    
    /**
     * 检查邮箱是否属于在职用户
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.status = '在职'")
    boolean existsByEmailAndActiveStatus(@Param("email") String email);
    
    /**
     * 查找需要同步到Keycloak的用户（邮箱不为空但没有Keycloak ID）
     */
    @Query("SELECT u FROM User u WHERE u.email IS NOT NULL AND u.email != '' AND u.keycloakId IS NULL AND u.status = '在职'")
    List<User> findUsersNeedingKeycloakSync();
    
    /**
     * 根据邮箱更新最后登录时间
     */
    @Query("UPDATE User u SET u.lastLoginTime = :loginTime WHERE u.email = :email")
    void updateLastLoginTimeByEmail(@Param("email") String email, @Param("loginTime") LocalDateTime loginTime);
    
    /**
     * 根据邮箱更新在线状态
     */
    @Query("UPDATE User u SET u.isOnline = :isOnline WHERE u.email = :email")
    void updateOnlineStatusByEmail(@Param("email") String email, @Param("isOnline") boolean isOnline);
    
    /**
     * 查找重复的邮箱（数据一致性检查）
     */
    @Query("SELECT u.email, COUNT(u) FROM User u GROUP BY u.email HAVING COUNT(u) > 1")
    List<Object[]> findDuplicateEmails();
    
    /**
     * 查找重复的Keycloak ID（数据一致性检查）
     */
    @Query("SELECT u.keycloakId, COUNT(u) FROM User u WHERE u.keycloakId IS NOT NULL GROUP BY u.keycloakId HAVING COUNT(u) > 1")
    List<Object[]> findDuplicateKeycloakIds();
    
    /**
     * 根据邮箱域名查找用户
     */
    @Query("SELECT u FROM User u WHERE u.email LIKE CONCAT('%@', :domain)")
    List<User> findByEmailDomain(@Param("domain") String domain);
    
    /**
     * 验证邮箱格式并查找用户 - 简化版本（邮箱格式验证移到服务层）
     */
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByValidEmail(@Param("email") String email);
    
    /**
     * 查找最近登录的用户（按邮箱排序）
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginTime IS NOT NULL ORDER BY u.lastLoginTime DESC, u.email ASC")
    List<User> findRecentlyLoggedInUsers(Pageable pageable);
    
    /**
     * 统计各邮箱域名的用户数量
     */
    @Query("SELECT SUBSTRING(u.email, LOCATE('@', u.email) + 1) as domain, COUNT(u) FROM User u WHERE u.email IS NOT NULL GROUP BY domain")
    List<Object[]> countUsersByEmailDomain();
} 