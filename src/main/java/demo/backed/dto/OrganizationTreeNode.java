package demo.backed.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.*;

@Data
@ApiModel(description = "组织架构树形节点DTO")
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrganizationTreeNode {
    
    @ApiModelProperty(value = "节点ID", required = true, example = "1")
    private String id;
    
    @ApiModelProperty(value = "节点名称", example = "技术部")
    private String name;
    
    @ApiModelProperty(value = "节点类型", example = "department", allowableValues = "company,department,position,person")
    private String type;
    
    @ApiModelProperty(value = "父节点ID", example = "0")
    private String parentId;
    
    @ApiModelProperty(value = "节点层级", example = "1")
    private Integer level;
    
    @ApiModelProperty(value = "节点代码", example = "TECH")
    private String code;
    
    @ApiModelProperty(value = "节点描述", example = "负责技术研发工作")
    private String description;
    
    @ApiModelProperty(value = "是否管理节点", example = "false")
    private Boolean isManager;
    
    @ApiModelProperty(value = "人员数量", example = "15")
    private Integer employeeCount;
    
    @ApiModelProperty(value = "状态", example = "正常")
    private String status;
    
    @ApiModelProperty(value = "排序权重", example = "1")
    private Integer sortOrder;
    
    @ApiModelProperty(value = "子节点列表")
    private List<OrganizationTreeNode> children;
    
    @ApiModelProperty(value = "扩展信息")
    private Map<String, Object> metadata;
    
    @ApiModelProperty(value = "是否展开", example = "true")
    private Boolean expanded;
    
    @ApiModelProperty(value = "是否选中", example = "false")
    private Boolean selected;
    
    @ApiModelProperty(value = "节点图标", example = "department")
    private String icon;
    
    @ApiModelProperty(value = "显示标签", example = "技术部 (15人)")
    private String label;
    
    @ApiModelProperty(value = "节点路径", example = "/1/2/3/")
    private String path;
    
    @ApiModelProperty(value = "是否为叶子节点", example = "false")
    private Boolean isLeaf;
    
    @ApiModelProperty(value = "职位/头衔", example = "技术总监")
    private String title;
    
    @ApiModelProperty(value = "上级管理者ID", example = "1")
    private Long managerId;
    
    @ApiModelProperty(value = "上级管理者名称", example = "张三")
    private String managerName;
    
    @ApiModelProperty(value = "额外信息")
    private Map<String, Object> extra;
    
    // 节点类型常量
    public static final String TYPE_COMPANY = "company";
    public static final String TYPE_DEPARTMENT = "department"; 
    public static final String TYPE_POSITION = "position";
    public static final String TYPE_PERSON = "person";
    
    // 构造函数
    public OrganizationTreeNode() {
        this.children = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.expanded = false;
        this.selected = false;
        this.isLeaf = false;
        this.employeeCount = 0;
        this.sortOrder = 0;
    }
    
    public OrganizationTreeNode(String id, String name, String type) {
        this();
        this.id = id;
        this.name = name;
        this.type = type;
        this.generateLabel();
        this.generateIcon();
    }
    
    /**
     * 添加子节点
     */
    public void addChild(OrganizationTreeNode child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        child.setParentId(this.id);
        this.children.add(child);
        // 更新叶子节点状态
        this.isLeaf = false;
    }
    
    /**
     * 移除子节点
     */
    public void removeChild(OrganizationTreeNode child) {
        if (this.children != null) {
            this.children.remove(child);
            // 更新叶子节点状态
            this.isLeaf = (this.children.isEmpty());
        }
    }
    
    /**
     * 根据ID查找子节点（非递归实现）
     * 使用@JsonIgnore防止序列化时调用
     */
    @JsonIgnore
    public OrganizationTreeNode findChildById(String id) {
        if (this.children == null) {
            return null;
        }
        
        // 使用队列进行广度优先搜索，避免递归
        Queue<OrganizationTreeNode> queue = new LinkedList<>();
        queue.offer(this);
        
        while (!queue.isEmpty()) {
            OrganizationTreeNode current = queue.poll();
            if (current.children != null) {
                for (OrganizationTreeNode child : current.children) {
                    if (id.equals(child.getId())) {
                        return child;
                    }
                    queue.offer(child);
                }
            }
        }
        return null;
    }
    
    /**
     * 生成显示标签
     */
    public void generateLabel() {
        if (this.name == null) {
            this.label = "未命名";
            return;
        }
        
        StringBuilder labelBuilder = new StringBuilder(this.name);
        
        // 根据类型添加不同的标签信息
        switch (this.type) {
            case TYPE_DEPARTMENT:
                if (this.employeeCount != null && this.employeeCount > 0) {
                    labelBuilder.append(" (").append(this.employeeCount).append("人)");
                }
                break;
            case TYPE_POSITION:
                if (this.employeeCount != null && this.employeeCount > 0) {
                    labelBuilder.append(" (").append(this.employeeCount).append("人在岗)");
                }
                break;
            case TYPE_PERSON:
                if (this.metadata != null && this.metadata.containsKey("position")) {
                    labelBuilder.append(" - ").append(this.metadata.get("position"));
                }
                break;
        }
        
        this.label = labelBuilder.toString();
    }
    
    /**
     * 生成节点图标
     */
    public void generateIcon() {
        switch (this.type) {
            case TYPE_COMPANY:
                this.icon = "building";
                break;
            case TYPE_DEPARTMENT:
                this.icon = "department";
                break;
            case TYPE_POSITION:
                this.icon = this.isManager != null && this.isManager ? "crown" : "user-circle";
                break;
            case TYPE_PERSON:
                this.icon = "user";
                break;
            default:
                this.icon = "folder";
                break;
        }
    }
    
    /**
     * 添加扩展信息
     */
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }
    
    /**
     * 获取扩展信息
     */
    public Object getMetadata(String key) {
        return this.metadata != null ? this.metadata.get(key) : null;
    }
    
    /**
     * 统计子节点总数（非递归实现）
     * 使用@JsonIgnore防止序列化时调用
     */
    @JsonIgnore
    public int getTotalChildCount() {
        int count = 0;
        if (this.children == null) {
            return count;
        }
        
        // 使用队列进行广度优先遍历，避免递归
        Queue<OrganizationTreeNode> queue = new LinkedList<>();
        queue.offer(this);
        
        while (!queue.isEmpty()) {
            OrganizationTreeNode current = queue.poll();
            if (current.children != null) {
                count += current.children.size();
                queue.addAll(current.children);
            }
        }
        return count;
    }
    
    /**
     * 获取所有叶子节点（非递归实现）
     * 使用@JsonIgnore防止序列化时调用
     */
    @JsonIgnore
    public List<OrganizationTreeNode> getLeafNodes() {
        List<OrganizationTreeNode> leafNodes = new ArrayList<>();
        
        // 使用栈进行深度优先遍历，避免递归
        Stack<OrganizationTreeNode> stack = new Stack<>();
        stack.push(this);
        
        while (!stack.isEmpty()) {
            OrganizationTreeNode current = stack.pop();
            
            if (current.children == null || current.children.isEmpty()) {
                leafNodes.add(current);
            } else {
                // 将子节点压入栈中
                for (int i = current.children.size() - 1; i >= 0; i--) {
                    stack.push(current.children.get(i));
                }
            }
        }
        return leafNodes;
    }
    
    // 便捷构造方法
    public static OrganizationTreeNode createDepartmentNode(String id, String name, Integer level, Integer employeeCount) {
        OrganizationTreeNode node = new OrganizationTreeNode();
        node.setId(id);
        node.setType(TYPE_DEPARTMENT);
        node.setName(name);
        node.setLevel(level);
        node.setEmployeeCount(employeeCount);
        node.generateLabel();
        node.generateIcon();
        return node;
    }
    
    public static OrganizationTreeNode createPositionNode(String id, String name, String level) {
        OrganizationTreeNode node = new OrganizationTreeNode();
        node.setId(id);
        node.setType(TYPE_POSITION);
        node.setName(name);
        node.setTitle(level);
        node.generateLabel();
        node.generateIcon();
        return node;
    }
    
    public static OrganizationTreeNode createEmployeeNode(String id, String name, String position) {
        OrganizationTreeNode node = new OrganizationTreeNode();
        node.setId(id);
        node.setType(TYPE_PERSON);
        node.setName(name);
        node.addMetadata("position", position);
        node.generateLabel();
        node.generateIcon();
        return node;
    }
} 