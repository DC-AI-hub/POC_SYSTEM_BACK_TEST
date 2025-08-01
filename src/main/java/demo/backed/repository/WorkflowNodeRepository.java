package demo.backed.repository;

import demo.backed.entity.WorkflowNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowNodeRepository extends JpaRepository<WorkflowNode, Long> {
    
    List<WorkflowNode> findByInstanceIdOrderByCreatedTimeAsc(Long instanceId);
    
    Optional<WorkflowNode> findByTaskId(String taskId);
    
    List<WorkflowNode> findByAssigneeIdAndStatus(Long assigneeId, String status);
    
    List<WorkflowNode> findByProxyIdAndStatus(Long proxyId, String status);
    
    @Query("SELECT n FROM WorkflowNode n WHERE (n.assigneeId = :userId OR n.proxyId = :userId) AND n.status = 'PENDING'")
    List<WorkflowNode> findPendingTasksByUser(@Param("userId") Long userId);
    
    @Query("SELECT n FROM WorkflowNode n WHERE n.instanceId = :instanceId AND n.status = 'COMPLETED' ORDER BY n.approvedTime DESC")
    List<WorkflowNode> findCompletedNodesByInstance(@Param("instanceId") Long instanceId);
    
    void deleteByInstanceId(Long instanceId);
    
    /**
     * 根据实例ID查找所有工作流节点
     */
    List<WorkflowNode> findByInstanceId(Long instanceId);
    
    /**
     * 根据实例ID和状态查找工作流节点
     */
    List<WorkflowNode> findByInstanceIdAndStatus(Long instanceId, String status);
} 