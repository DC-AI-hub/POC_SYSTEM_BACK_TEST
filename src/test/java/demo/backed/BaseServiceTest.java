package demo.backed;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Service层单元测试基础类
 * 提供通用的测试配置和工具方法
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
public abstract class BaseServiceTest {
    
    /**
     * 创建测试用的用户ID
     */
    protected Long createTestUserId() {
        return 1L;
    }
    
    /**
     * 创建测试用的部门ID
     */
    protected Long createTestDepartmentId() {
        return 1L;
    }
    
    /**
     * 创建测试用的组织ID
     */
    protected Long createTestOrganizationId() {
        return 1L;
    }
    
    /**
     * 创建测试用的申请编号
     */
    protected String createTestApplicationNumber() {
        return "TEST_APP_" + System.currentTimeMillis();
    }
} 