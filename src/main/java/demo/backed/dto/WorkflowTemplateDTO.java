package demo.backed.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * 工作流模板DTO
 */
@Data
@ApiModel(description = "工作流模板")
public class WorkflowTemplateDTO {
    
    @ApiModelProperty(value = "模板ID")
    private Long id;
    
    @ApiModelProperty(value = "模板名称", required = true)
    @NotBlank(message = "模板名称不能为空")
    private String name;
    
    @ApiModelProperty(value = "模板描述")
    private String description;
    
    @ApiModelProperty(value = "流程定义KEY")
    private String processKey;
    
    @ApiModelProperty(value = "模板类型", example = "expense")
    private String type;
    
    @ApiModelProperty(value = "状态", example = "draft")
    private String status;
    
    @ApiModelProperty(value = "BPMN XML内容")
    private String bpmnXml;
    
    @ApiModelProperty(value = "工作流配置数据（JSON格式）")
    private String configData;
    
    @ApiModelProperty(value = "模板版本号")
    private Integer templateVersion;
    
    @ApiModelProperty(value = "是否已部署")
    private Boolean isDeployed;
    
    @ApiModelProperty(value = "部署ID")
    private String deploymentId;
    
    @ApiModelProperty(value = "部署时间")
    private LocalDateTime deployedTime;
    
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdTime;
    
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedTime;
    
    @ApiModelProperty(value = "创建人")
    private String createdBy;
    
    @ApiModelProperty(value = "更新人")
    private String updatedBy;
} 