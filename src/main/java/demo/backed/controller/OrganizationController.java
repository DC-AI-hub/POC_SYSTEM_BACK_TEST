package demo.backed.controller;

import demo.backed.dto.ApiResponse;
import demo.backed.dto.OrganizationStats;
import demo.backed.dto.OrganizationTreeNode;
import demo.backed.entity.Department;
import demo.backed.entity.OrganizationRelation;
import demo.backed.entity.Position;
import demo.backed.service.OrganizationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 组织架构管理控制器
 */
@RestController
@RequestMapping("/api/organization")
@Api(tags = "组织架构管理")
@Slf4j
public class OrganizationController {
    
    @Autowired
    private OrganizationService organizationService;

    @GetMapping("/tree")
    @ApiOperation("获取完整组织架构树")
    public ApiResponse<OrganizationTreeNode> getOrganizationTree() {
        log.info("获取组织架构树");
        OrganizationTreeNode tree = organizationService.buildOrganizationTree();
        return ApiResponse.success("获取组织架构成功", tree);
    }
    
    @GetMapping("/tree/{deptId}")
    @ApiOperation("获取指定部门子树")
    public ApiResponse<OrganizationTreeNode> getDepartmentSubTree(@PathVariable Long deptId) {
        log.info("获取部门子树: {}", deptId);
        OrganizationTreeNode tree = organizationService.buildDepartmentSubTree(deptId);
        return ApiResponse.success("获取部门子树成功", tree);
    }
    
    @GetMapping("/departments")
    @ApiOperation("获取部门列表（分页）")
    public ApiResponse<Page<Department>> getDepartments(@PageableDefault(size = 20) Pageable pageable) {
        log.info("获取部门列表");
        Page<Department> departments = organizationService.getAllDepartments(pageable);
        return ApiResponse.success("获取部门列表成功", departments);
    }
    
    @GetMapping("/departments/simple")
    @ApiOperation("获取部门简单列表（不分页）")
    public ApiResponse<List<Map<String, Object>>> getSimpleDepartmentList() {
        log.info("获取部门简单列表");
        List<Map<String, Object>> departments = organizationService.getSimpleDepartmentList();
        return ApiResponse.success("获取部门列表成功", departments);
    }
    
    @PostMapping("/departments")
    @ApiOperation("创建部门")
    public ApiResponse<Department> createDepartment(@Valid @RequestBody Department department) {
        log.info("创建部门: {}", department.getName());
        Department created = organizationService.createDepartment(department);
        return ApiResponse.success("创建部门成功", created);
    }
    
    @PutMapping("/departments/{id}")
    @ApiOperation("更新部门")
    public ApiResponse<Department> updateDepartment(@PathVariable Long id, @RequestBody Department department) {
        log.info("更新部门: {}", id);
        Department updated = organizationService.updateDepartment(id, department);
        return ApiResponse.success("更新部门成功", updated);
    }
    
    @DeleteMapping("/departments/{id}")
    @ApiOperation("删除部门")
    public ApiResponse<Void> deleteDepartment(@PathVariable Long id) {
        log.info("删除部门: {}", id);
        organizationService.deleteDepartment(id);
        return ApiResponse.success("删除部门成功", null);
    }
    
    @GetMapping("/positions")
    @ApiOperation("获取岗位列表（分页）")
    public ApiResponse<Page<Position>> getPositions(@PageableDefault(size = 20) Pageable pageable) {
        log.info("获取岗位列表");
        Page<Position> positions = organizationService.getAllPositions(pageable);
        return ApiResponse.success("获取岗位列表成功", positions);
    }
    
    @PostMapping("/positions")
    @ApiOperation("创建岗位")
    public ApiResponse<Position> createPosition(@Valid @RequestBody Position position) {
        log.info("创建岗位: {}", position.getName());
        Position created = organizationService.createPosition(position);
        return ApiResponse.success("创建岗位成功", created);
    }
    
    @PutMapping("/positions/{id}")
    @ApiOperation("更新岗位")
    public ApiResponse<Position> updatePosition(@PathVariable Long id, @RequestBody Position position) {
        log.info("更新岗位: {}", id);
        Position updated = organizationService.updatePosition(id, position);
        return ApiResponse.success("更新岗位成功", updated);
    }
    
    @DeleteMapping("/positions/{id}")
    @ApiOperation("删除岗位")
    public ApiResponse<Void> deletePosition(@PathVariable Long id) {
        log.info("删除岗位: {}", id);
        organizationService.deletePosition(id);
        return ApiResponse.success("删除岗位成功", null);
    }
    
    @GetMapping("/stats")
    @ApiOperation("获取组织统计信息")
    public ApiResponse<OrganizationStats> getOrganizationStats() {
        log.info("获取组织统计信息");
        OrganizationStats stats = organizationService.getOrganizationStats();
        return ApiResponse.success("获取统计信息成功", stats);
    }
    


    /**
     * 获取简化的组织架构树（仅部门）
     */
    @GetMapping("/tree/simple")
    @ApiOperation("获取简化的组织架构树（仅部门）")
    public ApiResponse<OrganizationTreeNode> getSimpleOrganizationTree() {
        try {
            log.info("开始获取简化组织架构树...");
            OrganizationTreeNode tree = organizationService.getSimpleOrganizationTree();
            log.info("简化组织架构树获取成功");
            return ApiResponse.success(tree);
        } catch (Exception e) {
            log.error("获取简化组织架构树失败", e);
            return ApiResponse.error("获取简化组织架构树失败: " + e.getMessage());
        }
    }

    @GetMapping("/data/diagnose")
    @ApiOperation("诊断组织架构数据质量")
    public ApiResponse<Map<String, Object>> diagnoseOrganizationData() {
        try {
            log.info("开始诊断组织架构数据...");
            Map<String, Object> result = organizationService.diagnoseOrganizationData();
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("诊断组织架构数据失败", e);
            return ApiResponse.error("诊断失败: " + e.getMessage());
        }
    }

    @PostMapping("/initialize")
    @ApiOperation("从用户数据初始化组织架构")
    public ApiResponse<String> initializeOrganization() {
        try {
            log.info("开始从用户数据初始化组织架构...");
            organizationService.initializeOrganizationRelationsFromUsers();
            return ApiResponse.success("组织架构初始化成功");
        } catch (Exception e) {
            log.error("初始化组织架构失败", e);
            return ApiResponse.error("初始化失败: " + e.getMessage());
        }
    }

    // ==================== 组织关系管理接口 ====================

    /**
     * 更新组织关系
     */
    @PostMapping("/relations")
    @ApiOperation("更新组织关系")
    public ApiResponse<OrganizationRelation> updateOrganizationRelation(@Valid @RequestBody OrganizationRelation relation) {
        try {
            OrganizationRelation updated = organizationService.updateOrganizationRelation(relation);
            return ApiResponse.success("组织关系更新成功", updated);
        } catch (Exception e) {
            log.error("更新组织关系失败", e);
            return ApiResponse.error("更新组织关系失败: " + e.getMessage());
        }
    }

    /**
     * 批量更新组织关系
     */
    @PostMapping("/relations/batch")
    @ApiOperation("批量更新组织关系")
    public ApiResponse<List<OrganizationRelation>> batchUpdateOrganizationRelations(@RequestBody List<OrganizationRelation> relations) {
        try {
            List<OrganizationRelation> updated = organizationService.batchUpdateOrganizationRelations(relations);
            return ApiResponse.success("组织关系批量更新成功", updated);
        } catch (Exception e) {
            log.error("批量更新组织关系失败", e);
            return ApiResponse.error("批量更新组织关系失败: " + e.getMessage());
        }
    }

    /**
     * 获取员工组织关系
     */
    @GetMapping("/relations/employee/{employeeId}")
    @ApiOperation("获取员工组织关系")
    public ApiResponse<OrganizationRelation> getEmployeeOrganizationRelation(@PathVariable Long employeeId) {
        try {
            OrganizationRelation relation = organizationService.getEmployeeOrganizationRelation(employeeId);
            return ApiResponse.success("员工组织关系获取成功", relation);
        } catch (Exception e) {
            log.error("获取员工组织关系失败", e);
            return ApiResponse.error("获取员工组织关系失败: " + e.getMessage());
        }
    }

    /**
     * 获取部门员工列表
     */
    @GetMapping("/relations/department/{departmentId}/employees")
    @ApiOperation("获取部门员工列表")
    public ApiResponse<List<OrganizationRelation>> getDepartmentEmployees(@PathVariable Long departmentId) {
        try {
            List<OrganizationRelation> employees = organizationService.getDepartmentEmployees(departmentId);
            return ApiResponse.success("部门员工列表获取成功", employees);
        } catch (Exception e) {
            log.error("获取部门员工列表失败", e);
            return ApiResponse.error("获取部门员工列表失败: " + e.getMessage());
        }
    }

    /**
     * 从现有用户数据初始化组织关系
     */
    @PostMapping("/init-relations")
    @ApiOperation("从用户数据初始化组织关系")
    public ApiResponse<Void> initializeOrganizationRelations() {
        try {
            log.info("开始初始化组织关系...");
            organizationService.initializeOrganizationRelationsFromUsers();
            log.info("组织关系初始化完成");
            return ApiResponse.success("初始化成功", null);
        } catch (Exception e) {
            log.error("初始化组织关系失败: {}", e.getMessage());
            return ApiResponse.error("初始化组织关系失败: " + e.getMessage());
        }
    }

    @GetMapping("/tree/safe")
    @ApiOperation("获取安全的组织架构树（限制数据大小）")
    public ApiResponse<OrganizationTreeNode> getSafeOrganizationTree() {
        try {
            log.info("获取安全的组织架构树...");
            // 使用简化版本，限制数据大小
            OrganizationTreeNode tree = organizationService.getSimpleOrganizationTree();
            return ApiResponse.success(tree);
        } catch (Exception e) {
            log.error("获取安全的组织架构树失败", e);
            return ApiResponse.error("获取组织架构失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/cache/clear")
    @ApiOperation("清除组织架构树缓存")
    public ApiResponse<String> clearOrganizationTreeCache() {
        try {
            log.info("清除组织架构树缓存...");
            organizationService.clearOrganizationTreeCache();
            return ApiResponse.success("缓存清除成功");
        } catch (Exception e) {
            log.error("清除缓存失败", e);
            return ApiResponse.error("清除缓存失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/fix/circular-references")
    @ApiOperation("修复部门循环引用")
    public ApiResponse<Map<String, Object>> fixCircularReferences() {
        try {
            log.info("开始修复部门循环引用...");
            int fixedCount = organizationService.fixCircularReferences();
            
            Map<String, Object> result = new HashMap<>();
            result.put("fixedCount", fixedCount);
            result.put("message", fixedCount > 0 ? 
                    String.format("成功修复 %d 个部门的循环引用", fixedCount) : 
                    "未发现循环引用");
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("修复循环引用失败", e);
            return ApiResponse.error("修复失败: " + e.getMessage());
        }
    }
} 