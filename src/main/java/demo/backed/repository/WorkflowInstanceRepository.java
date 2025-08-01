package demo.backed.repository;

import demo.backed.entity.WorkflowInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {
    
    Optional<WorkflowInstance> findByProcessInstanceId(String processInstanceId);
    
    Optional<WorkflowInstance> findByBusinessTypeAndBusinessId(String businessType, String businessId);
    
    List<WorkflowInstance> findByApplicantIdOrderByStartTimeDesc(Long applicantId);
    
    List<WorkflowInstance> findByStatus(String status);
    
    @Query("SELECT w FROM WorkflowInstance w WHERE w.currentAssignee = :assignee AND w.status = 'RUNNING'")
    List<WorkflowInstance> findPendingByAssignee(@Param("assignee") String assignee);
    
    @Query("SELECT COUNT(w) FROM WorkflowInstance w WHERE w.status = :status")
    long countByStatus(@Param("status") String status);

    Page<WorkflowInstance> findByStatus(String status, Pageable pageable);
    
    Page<WorkflowInstance> findByApplicantId(Long applicantId, Pageable pageable);
    
    Page<WorkflowInstance> findByStatusAndApplicantId(String status, Long applicantId, Pageable pageable);
} 