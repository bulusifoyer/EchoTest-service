package com.testplatform.common;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密工具类
 * 使用BCrypt算法进行密码的加密存储和比对验证
 * BCrypt是一种基于Blowfish加密算法的密码哈希函数，具有以下特点：
 * 1. 每次加密生成的哈希值都不同
 * 2. 支持设置计算强度（工作因子）
 * 3. 内置盐值，无需单独存储
 * 4. 抗彩虹表攻击
 *
 * @author 测试平台开发团队
 * @since 2024-04-21
 */
public class PasswordUtils {

    /**
     * BCrypt密码编码器
     * 线程安全，可重复使用
     */
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /**
     * 对明文密码进行加密
     * @param rawPassword 明文密码
     * @return 加密后的密文密码
     */
    public static String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        // 使用BCrypt算法对密码进行加密
        return ENCODER.encode(rawPassword);
    }

    /**
     * 验证密码是否正确
     * @param rawPassword 明文密码（用户输入）
     * @param encodedPassword 密文密码（数据库中存储）
     * @return 密码是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        // 使用BCrypt算法验证密码是否匹配
        return ENCODER.matches(rawPassword, encodedPassword);
    }

    /**
     * 检查密码是否符合安全要求
     * @param password 待检查的密码
     * @return 密码是否符合要求
     */
    public static boolean isPasswordValid(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        // 密码长度至少6位
        if (password.length() < 6) {
            return false;
        }
        // 密码不能全是数字
        if (password.matches("^\\d+$")) {
            return false;
        }
        // 密码不能全是字母
        if (password.matches("^[a-zA-Z]+$")) {
            return false;
        }
        // 密码不能包含空格
        if (password.contains(" ")) {
            return false;
        }
        return true;
    }

    /**
     * 生成一个随机的加密密码
     * 用于测试或系统生成密码的场景
     * @param length 密码长度
     * @return 加密后的随机密码
     */
    public static String generateRandomPassword(int length) {
        // 密码字符集（不包含容易混淆的字符）
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            password.append(chars.charAt(index));
        }
        // 返回加密后的密码
        return encode(password.toString());
    }

    /**
     * 测试方法：验证BCrypt加密的一致性
     * 每次加密相同的密码都会得到不同的结果，但都能正确匹配
     */
    public static void main(String[] args) {
        String password = "123456";

        // 多次加密同一个密码
        String encoded1 = encode(password);
        String encoded2 = encode(password);
        String encoded3 = encode(password);

        System.out.println("原始密码: " + password);
        System.out.println("第一次加密: " + encoded1);
        System.out.println("第二次加密: " + encoded2);
        System.out.println("第三次加密: " + encoded3);

        // 验证是否都能正确匹配
        System.out.println("第一次加密结果匹配: " + matches(password, encoded1));
        System.out.println("第二次加密结果匹配: " + matches(password, encoded2));
        System.out.println("第三次加密结果匹配: " + matches(password, encoded3));

        // 测试密码强度验证
        System.out.println("测试密码强度验证:");
        System.out.println("123456: " + isPasswordValid("123456"));
        System.out.println("abcdef: " + isPasswordValid("abcdef"));
        System.out.println("abc123: " + isPasswordValid("abc123"));
        System.out.println("abc 123: " + isPasswordValid("abc 123"));
        System.out.println("Abc123!@#: " + isPasswordValid("Abc123!@#"));
    }
}