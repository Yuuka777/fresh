package com.xianhe.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 店员登录入参（《接口文档》2.1.1）
 */
@Data
public class StaffLoginDTO implements Serializable {

    //用户名
    private String username;

    //密码（明文传输，服务端转 MD5 后与库中密文比对）
    private String password;
}
