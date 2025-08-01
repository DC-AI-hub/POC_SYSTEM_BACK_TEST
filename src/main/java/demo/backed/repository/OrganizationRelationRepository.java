package demo.backed.repository;

import demo.backed.entity.OrganizationRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationRelationRepository extends JpaRepository<OrganizationRelation, Long> {
    
    /**
     * 根据岗位ID查询员工关系列表
     */
    @Query("SELECT r FROM OrganizationRelation r WHERE r.positionId = :positionId AND r.status = '生效' ORDER BY r.effectiveDate DESC")
    List<OrganizationRelation> findByPositionId(@Param("positionId") Long positionId);
    
    /**
     * 根据员工ID和状态查询组织关系
     */
    OrganizationRelation findByEmployeeIdAndStatus(Long employeeId, String status);
    
    /**
     * 根据部门ID和状态查询组织关系列表
     */
    List<OrganizationRelation> findByDepartmentIdAndStatus(Long departmentId, String status);
} 