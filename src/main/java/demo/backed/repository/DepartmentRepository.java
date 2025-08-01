package demo.backed.repository;

import demo.backed.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    
    /**
     * 根据部门代码查询部门
     */
    Optional<Department> findByCode(String code);
    
    /**
     * 根据父部门ID查询子部门列表
     */
    List<Department> findByParentId(Long parentId);
    
    /**
     * 根据状态查询部门列表
     */
    List<Department> findByStatus(String status);
    
    /**
     * 获取最大层级
     */
    @Query("SELECT MAX(d.level) FROM Department d")
    Integer findMaxLevel();
} 