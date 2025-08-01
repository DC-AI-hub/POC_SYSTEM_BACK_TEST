package demo.backed.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置类
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * 配置缓存管理器
     * 使用内存缓存（ConcurrentMapCache）
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("orgTree");
    }
} 