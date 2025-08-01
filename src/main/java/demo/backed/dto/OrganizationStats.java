package demo.backed.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ApiModel(description = "组织架构统计信息DTO")
public class OrganizationStats {
    
    @ApiModelProperty(value = "总部门数", example = "8")
    private Integer totalDepartments;
    
    @ApiModelProperty(value = "总岗位数", example = "25")
    private Integer totalPositions;
    
    @ApiModelProperty(value = "总员工数", example = "127")
    private Integer totalEmployees;
    
    @ApiModelProperty(value = "在职员工数", example = "120")
    private Integer activeEmployees;
    
    @ApiModelProperty(value = "各部门人数统计")
    private Map<String, Integer> departmentStats;
    
    @ApiModelProperty(value = "各层级统计")
    private Map<String, Integer> levelStats;
    
    @ApiModelProperty(value = "主要部门信息列表")
    private List<DepartmentInfo> topDepartments;
    
    @ApiModelProperty(value = "管理人员统计")
    private ManagerStats managerStats;
    
    @ApiModelProperty(value = "岗位统计")
    private PositionStats positionStats;
    
    @ApiModelProperty(value = "员工状态分布")
    private Map<String, Integer> employeeStatusStats;
    
    @ApiModelProperty(value = "统计时间")
    private LocalDateTime statisticsTime;
    
    @ApiModelProperty(value = "组织架构层级深度", example = "4")
    private Integer maxLevel;
    
    @ApiModelProperty(value = "平均部门人数", example = "15.8")
    private Double averageEmployeesPerDepartment;
    
    @ApiModelProperty(value = "人员配置率", example = "85.5")
    private Double staffingRate;
    
    @ApiModelProperty(value = "部门级别统计")
    private Map<String, Integer> positionLevelStats;
    
    // 构造函数
    public OrganizationStats() {
        this.departmentStats = new HashMap<>();
        this.levelStats = new HashMap<>();
        this.employeeStatusStats = new HashMap<>();
        this.statisticsTime = LocalDateTime.now();
        this.totalDepartments = 0;
        this.totalPositions = 0;
        this.totalEmployees = 0;
        this.activeEmployees = 0;
        this.maxLevel = 0;
    }
    
    /**
     * 计算人员配置率
     * 配置率 = 实际在职人数 / 总编制人数 * 100%
     */
    public void calculateStaffingRate(Integer totalHeadcount) {
        if (totalHeadcount != null && totalHeadcount > 0 && this.activeEmployees != null) {
            this.staffingRate = (double) this.activeEmployees / totalHeadcount * 100;
        } else {
            this.staffingRate = 0.0;
        }
    }
    
    /**
     * 计算平均部门人数
     */
    public void calculateAverageEmployeesPerDepartment() {
        if (this.totalDepartments != null && this.totalDepartments > 0 && this.activeEmployees != null) {
            this.averageEmployeesPerDepartment = (double) this.activeEmployees / this.totalDepartments;
        } else {
            this.averageEmployeesPerDepartment = 0.0;
        }
    }
    
    /**
     * 添加部门统计
     */
    public void addDepartmentStat(String departmentName, Integer count) {
        if (this.departmentStats == null) {
            this.departmentStats = new HashMap<>();
        }
        this.departmentStats.put(departmentName, count);
    }
    
    /**
     * 添加层级统计
     */
    public void addLevelStat(String level, Integer count) {
        if (this.levelStats == null) {
            this.levelStats = new HashMap<>();
        }
        this.levelStats.put(level, count);
    }
    
    /**
     * 添加员工状态统计
     */
    public void addEmployeeStatusStat(String status, Integer count) {
        if (this.employeeStatusStats == null) {
            this.employeeStatusStats = new HashMap<>();
        }
        this.employeeStatusStats.put(status, count);
    }
    
    // 内部类：部门信息
    @Data
    @ApiModel(description = "部门信息")
    public static class DepartmentInfo {
        @ApiModelProperty(value = "部门ID", example = "1")
        private Long id;
        
        @ApiModelProperty(value = "部门名称", example = "技术部")
        private String name;
        
        @ApiModelProperty(value = "部门代码", example = "TECH")
        private String code;
        
        @ApiModelProperty(value = "部门负责人", example = "张三")
        private String managerName;
        
        @ApiModelProperty(value = "员工人数", example = "25")
        private Integer employeeCount;
        
        @ApiModelProperty(value = "部门层级", example = "1")
        private Integer level;
        
        @ApiModelProperty(value = "部门状态", example = "正常")
        private String status;
        
        // 构造函数
        public DepartmentInfo() {}
        
        public DepartmentInfo(Long id, String name, String code, String managerName, Integer employeeCount) {
            this.id = id;
            this.name = name;
            this.code = code;
            this.managerName = managerName;
            this.employeeCount = employeeCount;
        }
    }
    
    // 内部类：管理人员统计
    @Data
    @ApiModel(description = "管理人员统计")
    public static class ManagerStats {
        @ApiModelProperty(value = "总管理人员数", example = "12")
        private Integer totalManagers;
        
        @ApiModelProperty(value = "部门负责人数", example = "8")
        private Integer departmentManagers;
        
        @ApiModelProperty(value = "团队负责人数", example = "15")
        private Integer teamLeaders;
        
        @ApiModelProperty(value = "管理跨度统计")
        private Map<String, Integer> managementSpan;
        
        public ManagerStats() {
            this.managementSpan = new HashMap<>();
            this.totalManagers = 0;
            this.departmentManagers = 0;
            this.teamLeaders = 0;
        }
    }
    
    // 内部类：岗位统计
    @Data
    @ApiModel(description = "岗位统计")
    public static class PositionStats {
        @ApiModelProperty(value = "管理岗位数", example = "12")
        private Integer managerPositions;
        
        @ApiModelProperty(value = "普通岗位数", example = "35")
        private Integer regularPositions;
        
        @ApiModelProperty(value = "空缺岗位数", example = "5")
        private Integer vacantPositions;
        
        @ApiModelProperty(value = "岗位级别分布")
        private Map<String, Integer> levelDistribution;
        
        @ApiModelProperty(value = "岗位满编率", example = "89.5")
        private Double occupancyRate;
        
        public PositionStats() {
            this.levelDistribution = new HashMap<>();
            this.managerPositions = 0;
            this.regularPositions = 0;
            this.vacantPositions = 0;
            this.occupancyRate = 0.0;
        }
    }
} 