package com.meitou.admin.config;

import com.meitou.admin.util.TokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器
 * 拦截请求并验证 JWT Token
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 获取 Authorization 头
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String token = authHeader.substring(7);
        Long userId = TokenUtil.getUserIdFromToken(token);
        String type = TokenUtil.getUserTypeFromToken(token);
        
        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Token 有效，设置 SecurityContext
            
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            if (type != null) {
                // 将 type 转换为角色，例如 user -> ROLE_USER, admin -> ROLE_ADMIN
                authorities.add(new SimpleGrantedAuthority("ROLE_" + type.toUpperCase()));
            } else {
                // 默认为 USER 角色
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }
            
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userId, null, authorities);
            
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
        
        filterChain.doFilter(request, response);
    }
}
