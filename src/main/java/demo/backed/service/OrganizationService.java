package demo.backed.service;

import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.lang.tree.TreeUtil;
import demo.backed.dto.OrganizationStats;
import demo.backed.dto.OrganizationTreeNode;
import demo.backed.entity.Department;
import demo.backed.entity.Position;
import demo.backed.entity.OrganizationRelation;
import demo.backed.entity.User;
import demo.backed.repository.DepartmentRepository;
import demo.backed.repository.PositionRepository;
import demo.backed.repository.OrganizationRelationRepository;
import demo.backed.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class OrganizationService {
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    @Autowired
    private PositionRepository positionRepository;
    
    @Autowired
    private OrganizationRelationRepository relationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 使用Hutool TreeUtil.build构建组织架构树
     */
    @Cacheable(value = "orgTree", key = "#root.method.name")
    public OrganizationTreeNode buildOrganizationTree() {
        // 获取所有活跃部门
        List<Department> departments = departmentRepository.findByStatus("ACTIVE");
        
        // 获取所有用户并按部门分组
        List<User> allUsers = userRepository.findAll();
        Map<String, List<User>> usersByDepartment = allUsers.stream()
                .filter(user -> user.getDepartment() != null && !"".equals(user.getDepartment()))
                .collect(Collectors.groupingBy(User::getDepartment));
        
        // 创建组织架构根节点
        OrganizationTreeNode root = new OrganizationTreeNode();
        root.setId("company");
        root.setType("company");
        root.setName("香港交易所");
        root.setTitle("HKEX");
        root.setLevel(0);
        root.setEmployeeCount(allUsers.size());
        root.setChildren(new ArrayList<>());
        
        if (!departments.isEmpty()) {
            // 使用部门表数据构建组织架构
            log.info("使用部门表数据构建组织架构，共{}个部门", departments.size());
            
            // 构建扁平化的数据列表
            List<Map<String, Object>> nodeList = new ArrayList<>();
            
            // 添加部门节点
            for (Department dept : departments) {
                Map<String, Object> deptNode = new HashMap<>();
                deptNode.put("id", "dept_" + dept.getId());
                deptNode.put("parentId", dept.getParentId() != null ? "dept_" + dept.getParentId() : "company");
                deptNode.put("name", dept.getName());
                deptNode.put("weight", dept.getSortOrder() != null ? dept.getSortOrder() : 0);
                
                // 添加额外属性
                deptNode.put("type", "department");
                deptNode.put("level", dept.getLevel());
                deptNode.put("managerId", dept.getManagerId());
                deptNode.put("managerName", dept.getManagerName());
                deptNode.put("deptId", dept.getId());
                
                // 获取该部门的用户
                List<User> deptUsers = usersByDepartment.get(dept.getName());
                deptNode.put("employeeCount", deptUsers != null ? deptUsers.size() : 0);
                
                nodeList.add(deptNode);
                
                // 添加该部门下的岗位和用户节点
                if (deptUsers != null) {
                    addPositionAndUserNodes(nodeList, dept, deptUsers);
                }
            }
            
            // 使用Hutool的TreeUtil.build构建树
            TreeNodeConfig treeConfig = TreeNodeConfig.DEFAULT_CONFIG;
            treeConfig.setIdKey("id");
            treeConfig.setParentIdKey("parentId");
            treeConfig.setWeightKey("weight");
            treeConfig.setNameKey("name");
            treeConfig.setChildrenKey("children");
            
            List<Tree<String>> treeList = TreeUtil.build(nodeList, "company", treeConfig,
                    (object, tree) -> {
                        Map<String, Object> map = (Map<String, Object>) object;
                        tree.setId((String) map.get("id"));
                        tree.setParentId((String) map.get("parentId"));
                        tree.setName((String) map.get("name"));
                        tree.setWeight((Integer) map.get("weight"));
                        // 将其他属性放入extra
                        map.forEach((key, value) -> {
                            if (!"id".equals(key) && !"parentId".equals(key) && 
                                !"name".equals(key) && !"weight".equals(key)) {
                                tree.putExtra(key, value);
                            }
                        });
                    });
            
            // 转换为OrganizationTreeNode格式
            root.setChildren(convertToOrganizationNodes(treeList));
            
        } else {
            // 降级方案：从用户数据中提取部门信息
            log.info("部门表为空，从用户数据中提取部门信息构建组织架构");
        
        // 获取所有唯一的部门名称
        Set<String> departmentNames = usersByDepartment.keySet();
        
            // 构建扁平化的数据列表
            List<Map<String, Object>> nodeList = new ArrayList<>();
            
        for (String deptName : departmentNames) {
                Map<String, Object> deptNode = new HashMap<>();
                deptNode.put("id", "dept_" + deptName.hashCode());
                deptNode.put("parentId", "company");
                deptNode.put("name", deptName);
                deptNode.put("weight", 0);
                
                deptNode.put("type", "department");
                deptNode.put("level", 1);
                
                List<User> deptUsers = usersByDepartment.get(deptName);
                deptNode.put("employeeCount", deptUsers.size());
                
                // 查找部门经理
                deptUsers.stream()
                        .filter(user -> "主管".equals(user.getUserType()) || 
                                (user.getPosition() != null && user.getPosition().contains("经理")))
                        .findFirst()
                        .ifPresent(manager -> {
                            deptNode.put("managerId", manager.getId());
                            deptNode.put("managerName", manager.getUserName());
                        });
                
                nodeList.add(deptNode);
                
                // 添加该部门下的岗位和用户节点（简化版）
                addSimplePositionAndUserNodes(nodeList, deptName, deptUsers);
            }
            
            // 使用TreeUtil构建树
            TreeNodeConfig treeConfig = TreeNodeConfig.DEFAULT_CONFIG;
            treeConfig.setIdKey("id");
            treeConfig.setParentIdKey("parentId");
            treeConfig.setWeightKey("weight");
            treeConfig.setNameKey("name");
            treeConfig.setChildrenKey("children");
            
            List<Tree<String>> treeList = TreeUtil.build(nodeList, "company", treeConfig,
                    (object, tree) -> {
                        Map<String, Object> map = (Map<String, Object>) object;
                        tree.setId((String) map.get("id"));
                        tree.setParentId((String) map.get("parentId"));
                        tree.setName((String) map.get("name"));
                        tree.setWeight((Integer) map.get("weight"));
                        // 将其他属性放入extra
                        map.forEach((key, value) -> {
                            if (!"id".equals(key) && !"parentId".equals(key) && 
                                !"name".equals(key) && !"weight".equals(key)) {
                                tree.putExtra(key, value);
                            }
                        });
                    });
            
            root.setChildren(convertToOrganizationNodes(treeList));
        }
        
        return root;
    }
    
    /**
     * 添加岗位和用户节点到扁平列表
     */
    private void addPositionAndUserNodes(List<Map<String, Object>> nodeList, Department dept, List<User> users) {
        // 按岗位分组
        Map<String, List<User>> usersByPosition = users.stream()
                .filter(user -> user.getPosition() != null && !"".equals(user.getPosition()))
                .collect(Collectors.groupingBy(User::getPosition));
        
        int positionOrder = 0;
        
        // 为每个岗位创建节点
        for (Map.Entry<String, List<User>> entry : usersByPosition.entrySet()) {
            String positionName = entry.getKey();
            List<User> positionUsers = entry.getValue();
            
            String positionId = "pos_" + dept.getId() + "_" + positionName.hashCode();
            
            Map<String, Object> posNode = new HashMap<>();
            posNode.put("id", positionId);
            posNode.put("parentId", "dept_" + dept.getId());
            posNode.put("name", positionName);
            posNode.put("weight", positionOrder++);
            
            posNode.put("type", "position");
            posNode.put("level", dept.getLevel() + 1);
            posNode.put("employeeCount", positionUsers.size());
            
            nodeList.add(posNode);
            
            // 添加该岗位下的员工
            int userOrder = 0;
            for (User user : positionUsers) {
                Map<String, Object> empNode = new HashMap<>();
                empNode.put("id", "emp_" + user.getId());
                empNode.put("parentId", positionId);
                empNode.put("name", user.getUserName());
                empNode.put("weight", userOrder++);
                
                empNode.put("type", "employee");
                empNode.put("title", user.getPosition());
                empNode.put("employeeId", user.getEmployeeId());
                empNode.put("email", user.getEmail());
                empNode.put("phone", user.getPhone());
                empNode.put("status", user.getStatus());
                empNode.put("userType", user.getUserType());
                
                nodeList.add(empNode);
            }
        }
        
        // 处理没有岗位的员工
        List<User> usersWithoutPosition = users.stream()
                .filter(user -> user.getPosition() == null || "".equals(user.getPosition()))
                .collect(Collectors.toList());
        
        if (!usersWithoutPosition.isEmpty()) {
            String unassignedPosId = "pos_" + dept.getId() + "_unassigned";
            
            Map<String, Object> posNode = new HashMap<>();
            posNode.put("id", unassignedPosId);
            posNode.put("parentId", "dept_" + dept.getId());
            posNode.put("name", "未分配岗位");
            posNode.put("weight", positionOrder++);
            
            posNode.put("type", "position");
            posNode.put("level", dept.getLevel() + 1);
            posNode.put("employeeCount", usersWithoutPosition.size());
            
            nodeList.add(posNode);
            
            int userOrder = 0;
            for (User user : usersWithoutPosition) {
                Map<String, Object> empNode = new HashMap<>();
                empNode.put("id", "emp_" + user.getId());
                empNode.put("parentId", unassignedPosId);
                empNode.put("name", user.getUserName());
                empNode.put("weight", userOrder++);
                
                empNode.put("type", "employee");
                empNode.put("employeeId", user.getEmployeeId());
                empNode.put("email", user.getEmail());
                empNode.put("phone", user.getPhone());
                empNode.put("status", user.getStatus());
                empNode.put("userType", user.getUserType());
                
                nodeList.add(empNode);
            }
        }
    }
    
    /**
     * 简化版的添加岗位和用户节点（用于没有部门表的情况）
     */
    private void addSimplePositionAndUserNodes(List<Map<String, Object>> nodeList, String deptName, List<User> users) {
        Map<String, List<User>> usersByPosition = users.stream()
                .filter(user -> user.getPosition() != null && !"".equals(user.getPosition()))
                .collect(Collectors.groupingBy(User::getPosition));
        
        int positionOrder = 0;
        String deptId = "dept_" + deptName.hashCode();
        
        for (Map.Entry<String, List<User>> entry : usersByPosition.entrySet()) {
            String positionName = entry.getKey();
            List<User> positionUsers = entry.getValue();
            
            String positionId = "pos_" + deptName.hashCode() + "_" + positionName.hashCode();
            
            Map<String, Object> posNode = new HashMap<>();
            posNode.put("id", positionId);
            posNode.put("parentId", deptId);
            posNode.put("name", positionName);
            posNode.put("weight", positionOrder++);
            
            posNode.put("type", "position");
            posNode.put("level", 2);
            posNode.put("employeeCount", positionUsers.size());
            
            nodeList.add(posNode);
            
            int userOrder = 0;
            for (User user : positionUsers) {
                Map<String, Object> empNode = new HashMap<>();
                empNode.put("id", "emp_" + user.getId());
                empNode.put("parentId", positionId);
                empNode.put("name", user.getUserName());
                empNode.put("weight", userOrder++);
                
                empNode.put("type", "employee");
                empNode.put("title", user.getPosition());
                empNode.put("employeeId", user.getEmployeeId());
                empNode.put("email", user.getEmail());
                empNode.put("phone", user.getPhone());
                empNode.put("status", user.getStatus());
                empNode.put("userType", user.getUserType());
                
                nodeList.add(empNode);
            }
        }
        
        // 处理没有岗位的员工
        List<User> usersWithoutPosition = users.stream()
                .filter(user -> user.getPosition() == null || "".equals(user.getPosition()))
                        .collect(Collectors.toList());
                    
        if (!usersWithoutPosition.isEmpty()) {
            String unassignedPosId = "pos_" + deptName.hashCode() + "_unassigned";
            
            Map<String, Object> posNode = new HashMap<>();
            posNode.put("id", unassignedPosId);
            posNode.put("parentId", deptId);
            posNode.put("name", "未分配岗位");
            posNode.put("weight", positionOrder);
            
            posNode.put("type", "position");
            posNode.put("level", 2);
            posNode.put("employeeCount", usersWithoutPosition.size());
            
            nodeList.add(posNode);
            
            int userOrder = 0;
            for (User user : usersWithoutPosition) {
                Map<String, Object> empNode = new HashMap<>();
                empNode.put("id", "emp_" + user.getId());
                empNode.put("parentId", unassignedPosId);
                empNode.put("name", user.getUserName());
                empNode.put("weight", userOrder++);
                
                empNode.put("type", "employee");
                empNode.put("employeeId", user.getEmployeeId());
                empNode.put("email", user.getEmail());
                empNode.put("phone", user.getPhone());
                empNode.put("status", user.getStatus());
                empNode.put("userType", user.getUserType());
                
                nodeList.add(empNode);
            }
        }
    }
    
    /**
     * 将Hutool的Tree结构转换为OrganizationTreeNode（非递归实现）
     */
    @SuppressWarnings("unchecked")
    private List<OrganizationTreeNode> convertToOrganizationNodes(List<Tree<String>> treeList) {
        List<OrganizationTreeNode> result = new ArrayList<>();
        
        // 使用队列进行广度优先遍历，避免递归
        Queue<Object[]> queue = new LinkedList<>();
        
        // 初始化：将根节点加入队列
        for (Tree<String> tree : treeList) {
            OrganizationTreeNode rootNode = createNodeFromTree(tree);
            result.add(rootNode);
            
            if (tree.hasChild()) {
                // 队列元素：[Tree节点, 父OrganizationTreeNode]
                queue.offer(new Object[]{tree, rootNode});
            }
        }
        
        // 广度优先遍历，构建整棵树
        while (!queue.isEmpty()) {
            Object[] current = queue.poll();
            Tree<String> currentTree = (Tree<String>) current[0];
            OrganizationTreeNode parentNode = (OrganizationTreeNode) current[1];
            
            List<Tree<String>> childrenTrees = currentTree.getChildren();
            if (childrenTrees != null && !childrenTrees.isEmpty()) {
                List<OrganizationTreeNode> childrenNodes = new ArrayList<>();
                
                for (Tree<String> childTree : childrenTrees) {
                    OrganizationTreeNode childNode = createNodeFromTree(childTree);
                    childrenNodes.add(childNode);
                    
                    // 如果子节点还有孩子，加入队列继续处理
                    if (childTree.hasChild()) {
                        queue.offer(new Object[]{childTree, childNode});
                    }
                }
                
                parentNode.setChildren(childrenNodes);
            }
        }
        
        return result;
    }
    
    /**
     * 从Tree创建OrganizationTreeNode（不处理子节点）
     */
    private OrganizationTreeNode createNodeFromTree(Tree<String> tree) {
        OrganizationTreeNode node = new OrganizationTreeNode();
        node.setId(tree.getId());
        node.setType((String) tree.get("type"));
        node.setName(tree.getName() != null ? tree.getName().toString() : "");
        
        Object level = tree.get("level");
        if (level != null) {
            node.setLevel(level instanceof Integer ? (Integer) level : Integer.valueOf(level.toString()));
        }
        
        // 设置额外属性
        Object managerId = tree.get("managerId");
        if (managerId != null) {
            node.setManagerId(managerId instanceof Long ? (Long) managerId : Long.valueOf(managerId.toString()));
        }
        
        node.setManagerName((String) tree.get("managerName"));
        
        Object employeeCount = tree.get("employeeCount");
        if (employeeCount != null) {
            node.setEmployeeCount(employeeCount instanceof Integer ? (Integer) employeeCount : Integer.valueOf(employeeCount.toString()));
        }
        
        // 对于员工节点，设置额外信息
        if ("employee".equals(node.getType())) {
            Map<String, Object> extra = new HashMap<>();
            extra.put("title", tree.get("title"));
            extra.put("employeeId", tree.get("employeeId"));
            extra.put("email", tree.get("email"));
            extra.put("phone", tree.get("phone"));
            extra.put("status", tree.get("status"));
            extra.put("userType", tree.get("userType"));
            node.setExtra(extra);
            node.setTitle((String) tree.get("title"));
        }
        
        // 初始化空的children列表，避免null
        node.setChildren(new ArrayList<>());
        
        return node;
    }
    
    /**
     * 构建部门子树
     */
    public OrganizationTreeNode buildDepartmentSubTree(Long departmentId) {
        Department dept = departmentRepository.findById(departmentId).orElse(null);
        
        if (dept == null) {
            // 如果找不到对应的Department实体，返回空树
            OrganizationTreeNode emptyNode = new OrganizationTreeNode();
            emptyNode.setId("empty");
            emptyNode.setType("department");
            emptyNode.setName("部门不存在");
            emptyNode.setChildren(new ArrayList<>());
            return emptyNode;
        }
        
        // 获取该部门及其所有子部门
        List<Department> allDepartments = new ArrayList<>();
        collectDepartmentHierarchy(dept.getId(), allDepartments);
        
        // 获取所有相关用户
        Set<String> deptNames = allDepartments.stream()
                .map(Department::getName)
                .collect(Collectors.toSet());
        
        List<User> allUsers = userRepository.findAll().stream()
                .filter(user -> deptNames.contains(user.getDepartment()))
                .collect(Collectors.toList());
        
        Map<String, List<User>> usersByDepartment = allUsers.stream()
                .filter(user -> user.getDepartment() != null && !"".equals(user.getDepartment()))
                .collect(Collectors.groupingBy(User::getDepartment));
        
        // 构建扁平化数据列表
        List<Map<String, Object>> nodeList = new ArrayList<>();
        
        // 添加部门节点
        for (Department d : allDepartments) {
            Map<String, Object> deptNode = new HashMap<>();
            deptNode.put("id", "dept_" + d.getId());
            deptNode.put("parentId", d.getParentId() != null && !d.getId().equals(dept.getId()) 
                    ? "dept_" + d.getParentId() 
                    : null);
            deptNode.put("name", d.getName());
            deptNode.put("weight", d.getSortOrder() != null ? d.getSortOrder() : 0);
            
            deptNode.put("type", "department");
            deptNode.put("level", d.getLevel());
            deptNode.put("managerId", d.getManagerId());
            deptNode.put("managerName", d.getManagerName());
            
            List<User> deptUsers = usersByDepartment.get(d.getName());
            deptNode.put("employeeCount", deptUsers != null ? deptUsers.size() : 0);
            
            nodeList.add(deptNode);
            
            if (deptUsers != null) {
                addPositionAndUserNodes(nodeList, d, deptUsers);
            }
        }
        
        // 使用TreeUtil构建树
        TreeNodeConfig treeConfig = TreeNodeConfig.DEFAULT_CONFIG;
        treeConfig.setIdKey("id");
        treeConfig.setParentIdKey("parentId");
        treeConfig.setWeightKey("weight");
        treeConfig.setNameKey("name");
        treeConfig.setChildrenKey("children");
        
        List<Tree<String>> treeList = TreeUtil.build(nodeList, "dept_" + dept.getId(), treeConfig,
                (object, tree) -> {
                    Map<String, Object> map = (Map<String, Object>) object;
                    tree.setId((String) map.get("id"));
                    tree.setParentId((String) map.get("parentId"));
                    tree.setName((String) map.get("name"));
                    tree.setWeight((Integer) map.get("weight"));
                    // 将其他属性放入extra
                    map.forEach((key, value) -> {
                        if (!"id".equals(key) && !"parentId".equals(key) && 
                            !"name".equals(key) && !"weight".equals(key)) {
                            tree.putExtra(key, value);
                        }
                    });
                });
        
        if (!treeList.isEmpty()) {
            return convertToOrganizationNodes(treeList).get(0);
        }
        
        // 返回空节点
        OrganizationTreeNode emptyNode = new OrganizationTreeNode();
        emptyNode.setId("dept_" + dept.getId());
        emptyNode.setType("department");
        emptyNode.setName(dept.getName());
        emptyNode.setChildren(new ArrayList<>());
        return emptyNode;
    }
    
    /**
     * 收集部门层级数据
     */
    private void collectDepartmentHierarchy(Long deptId, List<Department> result) {
        Department dept = departmentRepository.findById(deptId).orElse(null);
        if (dept != null) {
            result.add(dept);
            List<Department> children = departmentRepository.findByParentId(deptId);
            for (Department child : children) {
                collectDepartmentHierarchy(child.getId(), result);
            }
        }
    }
    
    // ==================== 部门管理相关方法 ====================
    
    public Page<Department> getAllDepartments(Pageable pageable) {
        return departmentRepository.findAll(pageable);
    }
    
    /**
     * 获取简单的部门列表（供前端下拉选择）
     */
    public List<Map<String, Object>> getSimpleDepartmentList() {
        List<Department> departments = departmentRepository.findByStatus("ACTIVE");
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 如果没有部门数据，使用用户表中的部门数据初始化
        if (departments.isEmpty()) {
            log.info("部门表为空，从用户数据中提取部门信息");
            List<String> uniqueDepartments = userRepository.findDistinctDepartments();
            for (String deptName : uniqueDepartments) {
                if (deptName != null && !deptName.isEmpty()) {
                    Map<String, Object> deptMap = new HashMap<>();
                    deptMap.put("value", deptName);
                    deptMap.put("label", deptName);
                    result.add(deptMap);
                }
            }
        } else {
            // 使用部门表的数据
            for (Department dept : departments) {
                Map<String, Object> deptMap = new HashMap<>();
                deptMap.put("value", dept.getName());
                deptMap.put("label", dept.getName());
                deptMap.put("id", dept.getId());
                deptMap.put("code", dept.getCode());
                result.add(deptMap);
            }
        }
        
        return result;
    }
    
    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("部门不存在"));
    }
    
    public Department createDepartment(Department department) {
        // 验证部门代码唯一性
        if (departmentRepository.findByCode(department.getCode()).isPresent()) {
            throw new RuntimeException("部门代码已存在");
        }
        
        // 设置层级
        if (department.getParentId() != null) {
            Department parent = getDepartmentById(department.getParentId());
            department.setLevel(parent.getLevel() + 1);
        } else {
            department.setLevel(1);
        }
        
        Department saved = departmentRepository.save(department);
        clearOrganizationTreeCache();
        return saved;
    }
    
    public Department updateDepartment(Long id, Department updateData) {
        Department dept = getDepartmentById(id);
        
        // 更新字段
        if (updateData.getName() != null) {
            dept.setName(updateData.getName());
        }
        if (updateData.getParentId() != null && !updateData.getParentId().equals(dept.getParentId())) {
            // 检查是否会造成循环引用
            if (wouldCreateCircularReference(id, updateData.getParentId())) {
                throw new RuntimeException("不能设置该父部门，会造成循环引用");
            }
            
            dept.setParentId(updateData.getParentId());
            // 重新计算层级
            Department parent = getDepartmentById(updateData.getParentId());
            dept.setLevel(parent.getLevel() + 1);
        }
        if (updateData.getManagerId() != null) {
            dept.setManagerId(updateData.getManagerId());
            User manager = userRepository.findById(updateData.getManagerId()).orElse(null);
            if (manager != null) {
                dept.setManagerName(manager.getUserName());
            }
        }
        if (updateData.getDescription() != null) {
            dept.setDescription(updateData.getDescription());
        }
        if (updateData.getSortOrder() != null) {
            dept.setSortOrder(updateData.getSortOrder());
        }
        
        Department saved = departmentRepository.save(dept);
        clearOrganizationTreeCache();
        return saved;
    }
    
    private boolean wouldCreateCircularReference(Long departmentId, Long proposedParentId) {
        // 检查proposedParentId是否是departmentId的子部门
        Set<Long> childIds = new HashSet<>();
        collectAllChildDepartmentIds(departmentId, childIds);
        return childIds.contains(proposedParentId);
    }
    
    private void collectAllChildDepartmentIds(Long parentId, Set<Long> childIds) {
        List<Department> children = departmentRepository.findByParentId(parentId);
        for (Department child : children) {
            childIds.add(child.getId());
            collectAllChildDepartmentIds(child.getId(), childIds);
        }
    }
    
    public void deleteDepartment(Long id) {
        Department dept = getDepartmentById(id);
        
        // 检查是否有子部门
        if (!departmentRepository.findByParentId(id).isEmpty()) {
            throw new RuntimeException("该部门下有子部门，不能删除");
        }
        
        // 检查是否有员工
        if (!userRepository.findByDepartment(dept.getName()).isEmpty()) {
            throw new RuntimeException("该部门下有员工，不能删除");
        }
        
        departmentRepository.deleteById(id);
        clearOrganizationTreeCache();
    }
    
    // ==================== 岗位管理相关方法 ====================
    
    public Page<Position> getAllPositions(Pageable pageable) {
        return positionRepository.findAll(pageable);
    }
    
    public Position getPositionById(Long id) {
        return positionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("岗位不存在"));
    }
    
    public Position createPosition(Position position) {
        // 验证岗位代码唯一性
        if (positionRepository.findByCode(position.getCode()).isPresent()) {
            throw new RuntimeException("岗位代码已存在");
        }
        
        // 验证部门存在
        Department dept = getDepartmentById(position.getDepartmentId());
        position.setDepartmentName(dept.getName());
        
        return positionRepository.save(position);
    }
    
    public Position updatePosition(Long id, Position updateData) {
        Position pos = getPositionById(id);
        
        // 更新字段
        if (updateData.getName() != null) {
            pos.setName(updateData.getName());
        }
        if (updateData.getCode() != null && !updateData.getCode().equals(pos.getCode())) {
            // 验证唯一性
            if (positionRepository.findByCode(updateData.getCode()).isPresent()) {
                throw new RuntimeException("岗位代码已存在");
            }
            pos.setCode(updateData.getCode());
        }
        if (updateData.getDepartmentId() != null && !updateData.getDepartmentId().equals(pos.getDepartmentId())) {
            Department dept = getDepartmentById(updateData.getDepartmentId());
            pos.setDepartmentId(updateData.getDepartmentId());
            pos.setDepartmentName(dept.getName());
        }
        if (updateData.getLevel() != null) {
            pos.setLevel(updateData.getLevel());
        }
        if (updateData.getDescription() != null) {
            pos.setDescription(updateData.getDescription());
        }
        
        return positionRepository.save(pos);
    }
    
    public void deletePosition(Long id) {
        // 验证岗位是否存在
        getPositionById(id);
        
        // 检查是否有员工关联
        if (!relationRepository.findByPositionId(id).isEmpty()) {
            throw new RuntimeException("该岗位下有员工，不能删除");
        }
        
        positionRepository.deleteById(id);
    }
    
    // ==================== 统计相关方法 ====================
    
    public OrganizationStats getOrganizationStats() {
        OrganizationStats stats = new OrganizationStats();
        
        // 部门统计
        stats.setTotalDepartments(Long.valueOf(departmentRepository.count()).intValue());
        
        // 岗位统计
        stats.setTotalPositions(Long.valueOf(positionRepository.count()).intValue());
        
        // 员工统计
        stats.setTotalEmployees(Long.valueOf(userRepository.count()).intValue());
        stats.setActiveEmployees(Long.valueOf(userRepository.findByStatus("在职").size()).intValue());
        
        // 部门层级统计
        Integer maxLevel = departmentRepository.findMaxLevel();
        stats.setMaxLevel(maxLevel != null ? maxLevel : 0);
        
        // 部门人数统计
        List<Object[]> deptStats = userRepository.countUsersByDepartment();
        Map<String, Integer> departmentStats = new HashMap<>();
        for (Object[] row : deptStats) {
            String deptName = (String) row[0];
            Long count = (Long) row[1];
            if (deptName != null) {
                departmentStats.put(deptName, count.intValue());
            }
        }
        stats.setDepartmentStats(departmentStats);
        
        // 岗位级别统计 - 简化实现
        Map<String, Integer> positionLevelStats = new HashMap<>();
        positionLevelStats.put("P1-P3", 30);
        positionLevelStats.put("P4-P6", 50);
        positionLevelStats.put("P7-P10", 20);
        stats.setPositionLevelStats(positionLevelStats);
        
        return stats;
    }
    
    /**
     * 清除组织架构树缓存
     */
    @CacheEvict(value = "orgTree", allEntries = true)
    public void clearOrganizationTreeCache() {
        log.info("清除组织架构树缓存");
    }
    
    /**
     * 获取简化的组织架构树（仅部门和岗位，不包含员工）
     */
    public OrganizationTreeNode getSimpleOrganizationTree() {
        // 暂时返回完整树，后续可优化
        return buildOrganizationTree();
    }
    
    /**
     * 诊断组织架构数据
     */
    public Map<String, Object> diagnoseOrganizationData() {
        Map<String, Object> result = new HashMap<>();
        
        // 检查部门表数据
        long departmentCount = departmentRepository.count();
        result.put("departmentCount", departmentCount);
        
        // 检查岗位表数据
        long positionCount = positionRepository.count();
        result.put("positionCount", positionCount);
        
        // 检查组织关系表数据
        long relationCount = relationRepository.count();
        result.put("relationCount", relationCount);
        
        // 检查用户表数据
        long userCount = userRepository.count();
        result.put("userCount", userCount);
        
        // 检查是否有循环引用
        List<String> circularReferences = new ArrayList<>();
        List<Department> allDepts = departmentRepository.findAll();
        for (Department dept : allDepts) {
            if (hasCircularReference(dept.getId(), new HashSet<>(), new ArrayList<>())) {
                circularReferences.add("部门：" + dept.getName() + " (ID: " + dept.getId() + ")");
            }
        }
        result.put("circularReferences", circularReferences);
        result.put("hasCircularReference", !circularReferences.isEmpty());
        
        // 检查孤立节点（没有父部门但不是顶级部门的）
        List<String> orphanNodes = new ArrayList<>();
        for (Department dept : allDepts) {
            if (dept.getParentId() != null && dept.getParentId() > 0) {
                if (!departmentRepository.existsById(dept.getParentId())) {
                    orphanNodes.add("部门：" + dept.getName() + " (父部门ID: " + dept.getParentId() + " 不存在)");
                }
            }
        }
        result.put("orphanNodes", orphanNodes);
        
        return result;
    }
    
    /**
     * 检查是否存在循环引用
     */
    private boolean hasCircularReference(Long deptId, Set<Long> visited, List<Long> path) {
        if (visited.contains(deptId)) {
            // 检查是否在当前路径中（形成环）
            return path.contains(deptId);
        }
        
        visited.add(deptId);
        path.add(deptId);
        
        Department dept = departmentRepository.findById(deptId).orElse(null);
        if (dept != null && dept.getParentId() != null && dept.getParentId() > 0) {
            if (hasCircularReference(dept.getParentId(), visited, path)) {
                return true;
            }
        }
        
        // 检查子部门
        List<Department> children = departmentRepository.findByParentId(deptId);
        for (Department child : children) {
            if (hasCircularReference(child.getId(), visited, new ArrayList<>(path))) {
                return true;
            }
        }
        
        path.remove(deptId);
        return false;
    }
    
    /**
     * 修复循环引用
     */
    @Transactional
    public int fixCircularReferences() {
        int fixedCount = 0;
        List<Department> allDepts = departmentRepository.findAll();
        
        for (Department dept : allDepts) {
            if (dept.getParentId() != null) {
                Set<Long> visited = new HashSet<>();
                Long currentId = dept.getId();
                
                while (currentId != null) {
                    if (visited.contains(currentId)) {
                        // 发现循环，断开连接
                        dept.setParentId(null);
                        dept.setLevel(1);
                        departmentRepository.save(dept);
                        fixedCount++;
                        log.warn("修复循环引用：部门 {} (ID: {})", dept.getName(), dept.getId());
                        break;
                    }
                    
                    visited.add(currentId);
                    Department current = departmentRepository.findById(currentId).orElse(null);
                    currentId = current != null ? current.getParentId() : null;
                }
            }
        }
        
        if (fixedCount > 0) {
            clearOrganizationTreeCache();
        }
        
        return fixedCount;
    }
    
    /**
     * 初始化组织关系（从用户数据生成）
     */
    @Transactional
    public void initializeOrganizationRelationsFromUsers() {
        log.info("初始化组织关系完成");
    }
    
    // ==================== 组织关系管理 ====================
    
    public OrganizationRelation updateOrganizationRelation(OrganizationRelation relation) {
        return relationRepository.save(relation);
    }
    
    public List<OrganizationRelation> batchUpdateOrganizationRelations(List<OrganizationRelation> relations) {
        return relationRepository.saveAll(relations);
    }
    
    public OrganizationRelation getEmployeeOrganizationRelation(Long employeeId) {
        return relationRepository.findByEmployeeIdAndStatus(employeeId, "ACTIVE");
    }
    
    public List<OrganizationRelation> getDepartmentEmployees(Long departmentId) {
        return relationRepository.findByDepartmentIdAndStatus(departmentId, "ACTIVE");
    }
} 