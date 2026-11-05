package com.xianhe.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 微信登录出参
 */
@Data
public class MemberLoginVO implements Serializable {

    //会员 id
    private Long id;

    //微信用户唯一标识
    private String openid;

    //JWT 令牌，前端存入本地存储并放入 authentication 请求头
    private String token;
}
