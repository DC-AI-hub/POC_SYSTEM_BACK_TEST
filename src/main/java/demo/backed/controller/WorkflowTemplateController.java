package demo.backed.controller;

import demo.backed.dto.ApiResponse;
import demo.backed.dto.WorkflowTemplateDTO;
import demo.backed.entity.WorkflowTemplate;
import demo.backed.service.WorkflowTemplateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作流模板控制器
 */
@RestController
@RequestMapping("/api/workflow/templates")
@Api(tags = "工作流模板管理")
@Slf4j
public class WorkflowTemplateController {
    
    @Autowired
    private WorkflowTemplateService templateService;
    
    /**
     * 获取所有工作流模板
     */
    @GetMapping
    @ApiOperation("获取所有工作流模板")
    public ApiResponse<List<WorkflowTemplateDTO>> getAllTemplates() {
        try {
            List<WorkflowTemplate> templates = templateService.getAllTemplates();
            List<WorkflowTemplateDTO> dtos = templates.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            return ApiResponse.success(dtos);
        } catch (Exception e) {
            log.error("获取工作流模板列表失败", e);
            return ApiResponse.error("获取工作流模板列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据ID获取工作流模板
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID获取工作流模板")
    public ApiResponse<WorkflowTemplateDTO> getTemplateById(@PathVariable Long id) {
        try {
            WorkflowTemplate template = templateService.getTemplateById(id);
            return ApiResponse.success(convertToDTO(template));
        } catch (Exception e) {
            log.error("获取工作流模板失败", e);
            return ApiResponse.error("获取工作流模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建工作流模板
     */
    @PostMapping
    @ApiOperation("创建工作流模板")
    public ApiResponse<WorkflowTemplateDTO> createTemplate(@Valid @RequestBody WorkflowTemplateDTO dto) {
        try {
            WorkflowTemplate template = convertToEntity(dto);
            WorkflowTemplate created = templateService.createTemplate(template);
            return ApiResponse.success("创建成功", convertToDTO(created));
        } catch (Exception e) {
            log.error("创建工作流模板失败", e);
            return ApiResponse.error("创建工作流模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新工作流模板
     */
    @PutMapping("/{id}")
    @ApiOperation("更新工作流模板")
    public ApiResponse<WorkflowTemplateDTO> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody WorkflowTemplateDTO dto) {
        try {
            WorkflowTemplate template = convertToEntity(dto);
            WorkflowTemplate updated = templateService.updateTemplate(id, template);
            return ApiResponse.success("更新成功", convertToDTO(updated));
        } catch (Exception e) {
            log.error("更新工作流模板失败", e);
            return ApiResponse.error("更新工作流模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除工作流模板
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除工作流模板")
    public ApiResponse<Void> deleteTemplate(@PathVariable Long id) {
        try {
            templateService.deleteTemplate(id);
            return ApiResponse.success("删除成功", null);
        } catch (Exception e) {
            log.error("删除工作流模板失败", e);
            return ApiResponse.error("删除工作流模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 部署工作流模板
     */
    @PostMapping("/{id}/deploy")
    @ApiOperation("部署工作流模板到Flowable")
    public ApiResponse<WorkflowTemplateDTO> deployTemplate(@PathVariable Long id) {
        try {
            WorkflowTemplate deployed = templateService.deployTemplate(id);
            return ApiResponse.success("部署成功", convertToDTO(deployed));
        } catch (Exception e) {
            log.error("部署工作流模板失败", e);
            return ApiResponse.error("部署工作流模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 取消部署工作流模板
     */
    @PostMapping("/{id}/undeploy")
    @ApiOperation("取消部署工作流模板")
    public ApiResponse<WorkflowTemplateDTO> undeployTemplate(@PathVariable Long id) {
        try {
            WorkflowTemplate undeployed = templateService.undeployTemplate(id);
            return ApiResponse.success("取消部署成功", convertToDTO(undeployed));
        } catch (Exception e) {
            log.error("取消部署工作流模板失败", e);
            return ApiResponse.error("取消部署工作流模板失败: " + e.getMessage());
        }
    }
    
    /**
     * 将实体转换为DTO
     */
    private WorkflowTemplateDTO convertToDTO(WorkflowTemplate template) {
        WorkflowTemplateDTO dto = new WorkflowTemplateDTO();
        BeanUtils.copyProperties(template, dto);
        return dto;
    }
    
    /**
     * 将DTO转换为实体
     */
    private WorkflowTemplate convertToEntity(WorkflowTemplateDTO dto) {
        WorkflowTemplate template = new WorkflowTemplate();
        BeanUtils.copyProperties(dto, template, "id", "createdTime", "updatedTime", "createdBy", "updatedBy");
        return template;
    }
} 