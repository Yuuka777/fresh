package com.xianhe.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置
 */
@Component
@ConfigurationProperties(prefix = "xianhe.jwt")
@Data
public class JwtProperties {

    //—— 管理端（店员）令牌 ——

    //签名密钥
    private String adminSecretKey;

    //有效期（毫秒）
    private long adminTtl;

    //前端传递令牌的请求头名称，管理端为 token
    private String adminTokenName;

    //—— 用户端（会员）令牌 ——

    //签名密钥
    private String userSecretKey;

    //有效期（毫秒）
    private long userTtl;

    //前端传递令牌的请求头名称，用户端为 authentication
    private String userTokenName;
}
