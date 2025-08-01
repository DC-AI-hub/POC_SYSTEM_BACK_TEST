package demo.backed.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    
    @Autowired
    private DataSource dataSource;
    
    @GetMapping("/check")
    public Map<String, Object> healthCheck() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", System.currentTimeMillis());
        
        // 检查数据库连接
        try (Connection conn = dataSource.getConnection()) {
            result.put("database", "Connected");
            result.put("databaseUrl", conn.getMetaData().getURL());
        } catch (Exception e) {
            result.put("database", "Failed");
            result.put("error", e.getMessage());
        }
        
        // 检查Flowable
        try {
            result.put("flowable", "Available");
        } catch (Exception e) {
            result.put("flowable", "Not Available");
        }
        
        return result;
    }
} 