package com.meitou.admin.util;

/**
 * Token工具类
 * 用于解析和验证Token
 */
public class TokenUtil {
    
    /**
     * 从Token中提取用户ID
     * Token格式：app_token_{userId}_{timestamp}
     * 
     * @param token Token字符串
     * @return 用户ID，如果解析失败返回null
     */
    public static Long getUserIdFromToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        // 移除Bearer前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        // 解析Token格式：app_token_{userId}_{timestamp}
        if (token.startsWith("app_token_")) {
            String[] parts = token.split("_");
            if (parts.length >= 3) {
                try {
                    return Long.parseLong(parts[2]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 验证Token格式是否有效
     * 
     * @param token Token字符串
     * @return 是否有效
     */
    public static boolean isValidToken(String token) {
        return getUserIdFromToken(token) != null;
    }
}

