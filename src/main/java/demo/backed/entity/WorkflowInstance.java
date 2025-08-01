package demo.backed.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "t_poc_workflow_instances")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkflowInstance extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "process_instance_id", unique = true)
    private String processInstanceId;
    
    @Column(name = "business_type", length = 50)
    private String businessType;
    
    @Column(name = "business_id", length = 50)
    private String businessId;
    
    @Column(name = "title", length = 200)
    private String title;
    
    @Column(name = "status", length = 20)
    private String status; // RUNNING, COMPLETED, SUSPENDED, TERMINATED
    
    @Column(name = "applicant_id")
    private Long applicantId;
    
    @Column(name = "applicant_name", length = 50)
    private String applicantName;
    
    @Column(name = "start_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
    
    @Column(name = "variables", columnDefinition = "TEXT")
    private String variables;
    
    @Column(name = "current_node_name", length = 100)
    private String currentNodeName;
    
    @Column(name = "current_assignee", length = 100)
    private String currentAssignee;
} 