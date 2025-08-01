package demo.backed.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstanceDTO {
    private Long id;
    private String processInstanceId;
    private String businessType;
    private String businessId;
    private String title;
    private String status;
    private String statusText;
    private Long applicantId;
    private String applicantName;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
    
    private String currentNodeName;
    private String currentAssignee;
    private Map<String, Object> variables;
} 