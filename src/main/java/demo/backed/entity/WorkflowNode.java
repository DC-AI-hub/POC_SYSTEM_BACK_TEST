package demo.backed.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "t_poc_workflow_nodes")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkflowNode extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "instance_id")
    private Long instanceId;
    
    @Column(name = "task_id", unique = true)
    private String taskId;
    
    @Column(name = "node_key", length = 50)
    private String nodeKey;
    
    @Column(name = "node_name", length = 100)
    private String nodeName;
    
    @Column(name = "status", length = 20)
    private String status; // PENDING, IN_PROGRESS, COMPLETED, REJECTED, RETURNED
    
    @Column(name = "assignee_id")
    private Long assigneeId;
    
    @Column(name = "assignee_name", length = 50)
    private String assigneeName;
    
    @Column(name = "proxy_id")
    private Long proxyId;
    
    @Column(name = "proxy_name", length = 50)
    private String proxyName;
    
    @Column(name = "approved_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime approvedTime;
    
    @Column(name = "comment", length = 500)
    private String comment;
    
    @Column(name = "is_returned")
    private Boolean isReturned = false;
    
    @Column(name = "execution_id")
    private String executionId;
    
    @Column(name = "due_date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dueDate;
} 