package demo.backed.config;

import demo.backed.entity.User;
import demo.backed.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Profile("!keycloak")  // 只在非 keycloak profile 时加载
public class EmployeeAuthenticationService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 根据邮箱查找用户
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (!userOptional.isPresent()) {
            throw new UsernameNotFoundException("员工不存在: " + email);
        }
        
        User user = userOptional.get();
        
        // 检查用户状态 - 只允许在职员工登录
        if (!"在职".equals(user.getStatus())) {
            throw new UsernameNotFoundException("员工已离职或状态异常，无法登录: " + email);
        }
        
        // 构建UserDetails对象
        return buildUserDetails(user);
    }
    
    /**
     * 构建UserDetails对象
     */
    private UserDetails buildUserDetails(User user) {
        // 根据用户类型设置权限
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        
        // 基础用户权限
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        // 根据用户类型添加额外权限
        if ("主管".equals(user.getUserType())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_MANAGER"));
        }
        
        // 根据部门添加权限（如果需要）
        if ("财务部".equals(user.getDepartment())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_FINANCE"));
        }
        
        if ("人事部".equals(user.getDepartment())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_HR"));
        }
        
        // 创建UserDetails对象
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
    
    /**
     * 验证用户是否可以登录
     */
    public boolean canUserLogin(String email) {
        try {
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (!userOptional.isPresent()) {
                return false;
            }
            
            User user = userOptional.get();
            return "在职".equals(user.getStatus());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取用户的完整信息（用于JWT生成）
     */
    public User getFullUserInfo(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (!userOptional.isPresent()) {
            throw new UsernameNotFoundException("员工不存在: " + email);
        }
        
        User user = userOptional.get();
        if (!"在职".equals(user.getStatus())) {
            throw new UsernameNotFoundException("员工已离职或状态异常: " + email);
        }
        
        return user;
    }
} 