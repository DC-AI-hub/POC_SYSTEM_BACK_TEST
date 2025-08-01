package demo.backed.repository;

import demo.backed.entity.WorkflowTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 工作流模板Repository
 */
@Repository
public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplate, Long> {
    
    /**
     * 根据流程Key查找模板
     */
    Optional<WorkflowTemplate> findByProcessKey(String processKey);
    
    /**
     * 查找所有已启用的模板
     */
    List<WorkflowTemplate> findByStatus(String status);
    
    /**
     * 根据类型查找模板
     */
    List<WorkflowTemplate> findByType(String type);
    
    /**
     * 检查流程Key是否已存在
     */
    boolean existsByProcessKey(String processKey);
    
    /**
     * 查找未删除的模板
     */
    @Query("SELECT t FROM WorkflowTemplate t WHERE t.isDeleted = false ORDER BY t.createdTime DESC")
    List<WorkflowTemplate> findAllActive();
    
    /**
     * 根据部署ID查找模板
     */
    Optional<WorkflowTemplate> findByDeploymentId(String deploymentId);
    
    /**
     * 根据部署状态和状态查找模板
     */
    List<WorkflowTemplate> findByIsDeployedAndStatus(Boolean isDeployed, String status);
    
    /**
     * 根据类型和部署状态查找模板
     */
    List<WorkflowTemplate> findByTypeAndIsDeployedAndStatus(String type, Boolean isDeployed, String status);
} 