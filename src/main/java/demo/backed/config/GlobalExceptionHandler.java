package demo.backed.config;

import demo.backed.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    

    
    /**
     * 处理实体不存在异常
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleEntityNotFoundException(EntityNotFoundException e) {
        logger.error("实体不存在: ", e);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.notFound(e.getMessage()));
    }
    
    /**
     * 处理数据完整性违反异常（如唯一约束）
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        logger.error("数据完整性违反: ", e);
        String message = "数据操作失败，可能存在重复数据或违反约束条件";
        String exceptionMessage = e.getMessage();
        if (exceptionMessage != null && exceptionMessage.contains("duplicate key")) {
            message = "数据已存在，不能重复添加";
        }
        return ResponseEntity.badRequest().body(ApiResponse.badRequest(message));
    }
    
    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException e) {
        logger.error("参数验证失败: ", e);
        StringBuilder errorMsg = new StringBuilder("参数验证失败: ");
        e.getBindingResult().getFieldErrors().forEach(error -> 
            errorMsg.append(error.getField()).append(" ").append(error.getDefaultMessage()).append("; ")
        );
        return ResponseEntity.badRequest().body(ApiResponse.badRequest(errorMsg.toString()));
    }
    
    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        logger.error("参数类型不匹配: ", e);
        String message = String.format("参数 '%s' 的值 '%s' 类型不正确", e.getName(), e.getValue());
        return ResponseEntity.badRequest().body(ApiResponse.badRequest(message));
    }
    
    /**
     * 处理JWT认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(AuthenticationException e, HttpServletRequest request) {
        logger.error("JWT认证异常: {}, 请求路径: {}", e.getMessage(), request.getRequestURI());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "AUTHENTICATION_FAILED");
        errorDetails.put("message", "认证失败，请重新登录");
        errorDetails.put("requestPath", request.getRequestURI());
        errorDetails.put("timestamp", System.currentTimeMillis());
        
        if (e instanceof BadCredentialsException) {
            errorDetails.put("message", "用户名或密码错误");
        }
        
        ApiResponse<Object> response = new ApiResponse<>(401, (String) errorDetails.get("message"), errorDetails);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    /**
     * 处理权限拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        logger.error("权限拒绝: {}, 请求路径: {}", e.getMessage(), request.getRequestURI());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "ACCESS_DENIED");
        errorDetails.put("message", "权限不足，无法访问此资源");
        errorDetails.put("requestPath", request.getRequestURI());
        errorDetails.put("timestamp", System.currentTimeMillis());
        
        ApiResponse<Object> response = new ApiResponse<>(403, "权限不足，无法访问此资源", errorDetails);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
    
    /**
     * 处理数据库连接异常
     */
    @ExceptionHandler({DataAccessException.class, SQLException.class})
    public ResponseEntity<ApiResponse<Object>> handleDatabaseException(Exception e, HttpServletRequest request) {
        logger.error("数据库异常: {}, 请求路径: {}", e.getMessage(), request.getRequestURI());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "DATABASE_ERROR");
        errorDetails.put("message", "数据库连接异常，请稍后重试");
        errorDetails.put("requestPath", request.getRequestURI());
        errorDetails.put("timestamp", System.currentTimeMillis());
        
        String message = "数据库操作失败，请稍后重试";
        if (e.getMessage() != null) {
            if (e.getMessage().contains("Connection") || e.getMessage().contains("connection")) {
                message = "数据库连接失败，请检查网络连接";
            } else if (e.getMessage().contains("timeout")) {
                message = "数据库操作超时，请稍后重试";
            }
        }
        
        errorDetails.put("message", message);
        
        ApiResponse<Object> response = new ApiResponse<>(503, message, errorDetails);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
    
    /**
     * 处理业务逻辑异常（自定义）
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(RuntimeException e, HttpServletRequest request) {
        logger.error("业务逻辑异常: {}, 请求路径: {}", e.getMessage(), request.getRequestURI());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "BUSINESS_ERROR");
        errorDetails.put("requestPath", request.getRequestURI());
        errorDetails.put("timestamp", System.currentTimeMillis());
        
        String message = e.getMessage();
        
        // 根据异常信息分类
        if (message != null) {
            if (message.contains("用户未认证") || message.contains("Token")) {
                errorDetails.put("error", "AUTHENTICATION_REQUIRED");
                message = "用户未认证，请重新登录";
                ApiResponse<Object> response = new ApiResponse<>(401, message, errorDetails);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            } else if (message.contains("权限") || message.contains("无权限")) {
                errorDetails.put("error", "PERMISSION_DENIED");
                message = "权限不足，无法执行此操作";
                ApiResponse<Object> response = new ApiResponse<>(403, message, errorDetails);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            } else if (message.contains("不存在") || message.contains("未找到")) {
                errorDetails.put("error", "RESOURCE_NOT_FOUND");
                ApiResponse<Object> response = new ApiResponse<>(404, message, errorDetails);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        }
        
        // 默认业务异常
        errorDetails.put("message", message);
        ApiResponse<Object> response = new ApiResponse<>(400, message, errorDetails);
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * 处理参数约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        logger.error("参数约束违反: {}, 请求路径: {}", e.getMessage(), request.getRequestURI());
        
        StringBuilder errorMsg = new StringBuilder("参数验证失败: ");
        e.getConstraintViolations().forEach(violation -> {
            errorMsg.append(violation.getPropertyPath()).append(" ").append(violation.getMessage()).append("; ");
        });
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "VALIDATION_FAILED");
        errorDetails.put("message", errorMsg.toString());
        errorDetails.put("requestPath", request.getRequestURI());
        errorDetails.put("timestamp", System.currentTimeMillis());
        
        ApiResponse<Object> response = new ApiResponse<>(400, errorMsg.toString(), errorDetails);
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * 处理工作流异常
     */
    @ExceptionHandler({org.flowable.common.engine.api.FlowableException.class})
    public ResponseEntity<ApiResponse<Object>> handleWorkflowException(Exception e, HttpServletRequest request) {
        logger.error("工作流异常: {}, 请求路径: {}", e.getMessage(), request.getRequestURI());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "WORKFLOW_ERROR");
        errorDetails.put("requestPath", request.getRequestURI());
        errorDetails.put("timestamp", System.currentTimeMillis());
        
        String message = "工作流操作失败";
        if (e.getMessage() != null) {
            if (e.getMessage().contains("Task")) {
                message = "任务操作失败，请检查任务状态";
            } else if (e.getMessage().contains("Process")) {
                message = "流程操作失败，请检查流程状态";
            } else if (e.getMessage().contains("not found")) {
                message = "工作流资源不存在";
            }
        }
        
        errorDetails.put("message", message);
        
        ApiResponse<Object> response = new ApiResponse<>(400, message, errorDetails);
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(Exception e, HttpServletRequest request) {
        logger.error("系统异常: {}, 请求路径: {}", e.getMessage(), request.getRequestURI(), e);
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "INTERNAL_SERVER_ERROR");
        errorDetails.put("message", "系统内部错误，请联系管理员");
        errorDetails.put("requestPath", request.getRequestURI());
        errorDetails.put("timestamp", System.currentTimeMillis());
        errorDetails.put("exceptionType", e.getClass().getSimpleName());
        
        ApiResponse<Object> response = new ApiResponse<>(500, "系统内部错误，请联系管理员", errorDetails);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
} 