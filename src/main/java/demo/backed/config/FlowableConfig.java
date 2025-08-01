package demo.backed.config;

import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import javax.sql.DataSource;

@Configuration
public class FlowableConfig {
    
    @Autowired
    private DataSource dataSource;
    
    /**
     * 自定义Flowable流程引擎配置
     */
    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> processEngineConfigurer() {
        return processEngineConfiguration -> {
            // 设置数据源
            processEngineConfiguration.setDataSource(dataSource);
            
            // 设置数据库schema更新策略
            processEngineConfiguration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
            
            // 设置异步执行器
            processEngineConfiguration.setAsyncExecutorActivate(false);
            
            // 设置流程定义缓存限制
            processEngineConfiguration.setProcessDefinitionCacheLimit(100);
            
            // 设置历史级别
            processEngineConfiguration.setHistory("full");
        };
    }
} 