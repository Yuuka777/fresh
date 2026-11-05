package com.xianhe.utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 口令哈希：PBKDF2-HmacSHA256 + 每账号随机盐（JDK 自带，不引入额外依赖）
 *
 * 存储格式：pbkdf2$迭代次数$盐Base64$哈希Base64
 * 兼容旧数据：库中若仍是 32 位 MD5，判定通过后由调用方升级为新格式，不需要停机迁移
 */
public final class PasswordUtil {

    private static final String PREFIX = "pbkdf2";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /**
     * 生成口令哈希
     * @param rawPassword
     * @return
     */
    public static String encode(String rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(text(rawPassword), salt, ITERATIONS, KEY_BITS);
        return PREFIX + "$" + ITERATIONS + "$" + b64(salt) + "$" + b64(hash);
    }

    /**
     * 校验口令，兼容旧的 MD5 字符串
     * @param rawPassword
     * @param stored
     * @return
     */
    public static boolean matches(String rawPassword, String stored) {
        if (stored == null || stored.isEmpty()) {
            return false;
        }
        if (!isModern(stored)) {
            // 旧格式：32 位 MD5。这里保留兼容只为让存量账号能登录，登录成功后即升级
            return md5(text(rawPassword)).equals(stored);
        }

        String[] parts = stored.split("\\$");
        if (parts.length != 4) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(text(rawPassword), salt, iterations, expected.length * 8);
            // 定长比较，避免按字节短路带来的时序差异
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 是否是当前格式（false = 旧的 MD5，登录成功后应升级）
     * @param stored
     * @return
     */
    public static boolean isModern(String stored) {
        return stored != null && stored.startsWith(PREFIX + "$");
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations, int keyBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyBits);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new IllegalStateException("口令哈希计算失败", ex);
        }
    }

    private static String md5(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("MD5 计算失败", ex);
        }
    }

    private static String b64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }
}
