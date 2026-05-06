package com.testplatform.common;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 用于生成和解析JWT令牌，实现用户身份认证
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
@Slf4j
@Component
public class JwtUtils {

    /**
     * JWT密钥，从配置文件中读取
     * 用于签名和验证JWT，必须保密
     */
    @Value("${jwt.secret:mySecretKey12345678901234567890123456789012}")
    private String secret;

    /**
     * JWT过期时间（毫秒），从配置文件中读取
     * 默认24小时
     */
    @Value("${jwt.expiration:86400000}")
    private Long expiration;


    /**
     * 生成JWT令牌
     * @param username 用户名
     * @param userId 用户ID
     * @return JWT令牌
     */
    public String generateToken(String username, Long userId) {
        // 设置令牌过期时间
        Date expirationDate = new Date(System.currentTimeMillis() + expiration);

        // 创建自定义声明，存储用户ID
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);

        // 构建JWT
        return Jwts.builder()
                .setClaims(claims)                    // 设置自定义声明
                .setSubject(username)                 // 设置主题（用户名）
                .setIssuedAt(new Date())              // 设置签发时间
                .setExpiration(expirationDate)        // 设置过期时间
                .signWith(SignatureAlgorithm.HS256, secret.getBytes()) // 设置签名算法和密钥
                .compact();                           // 生成JWT字符串
    }

    /**
     * 从JWT令牌中获取用户名
     * @param token JWT令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 从JWT令牌中获取用户ID
     * @param token JWT令牌
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Integer) {
            return ((Integer) userIdObj).longValue();
        } else if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        } else if (userIdObj instanceof Number) {
            return ((Number) userIdObj).longValue();
        } else {
            throw new IllegalArgumentException("JWT令牌中的用户ID格式不正确: " + userIdObj);
        }
    }

    /**
     * 验证JWT令牌是否有效
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            // 解析令牌，如果解析成功则说明令牌有效
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            // 令牌已过期
            log.error("JWT令牌已过期: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            // 令牌无效
            log.error("JWT令牌无效: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            // 其他异常
            log.error("验证JWT令牌时发生错误: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析JWT令牌
     * @param token JWT令牌
     * @return 声明对象
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret.getBytes())  // 设置签名密钥
                .parseClaimsJws(token)            // 解析JWT
                .getBody();                       // 获取载荷
    }

    /**
     * 检查JWT令牌是否即将过期
     * @param token JWT令牌
     * @return 是否即将过期（剩余时间小于10分钟）
     */
    public boolean isTokenExpiringSoon(String token) {
        Claims claims = parseToken(token);
        Date expiration = claims.getExpiration();
        // 获取剩余时间（毫秒）
        long remainingTime = expiration.getTime() - System.currentTimeMillis();
        // 如果剩余时间小于10分钟，则认为即将过期
        return remainingTime < 600000;
    }

    /**
     * 刷新JWT令牌
     * @param oldToken 旧的JWT令牌
     * @return 新的JWT令牌
     */
    public String refreshToken(String oldToken) {
        // 从旧令牌中获取用户信息
        String username = getUsernameFromToken(oldToken);
        Long userId = getUserIdFromToken(oldToken);
        // 生成新令牌
        return generateToken(username, userId);
    }
}