package com.xianhe.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 修改密码入参（《接口文档》2.1.8）
 */
@Data
public class PasswordEditDTO implements Serializable {

    //店员 id
    private Long empId;

    //原密码（明文，服务端转 MD5 比对）
    private String oldPassword;

    //新密码（明文，服务端转 MD5 存储）
    private String newPassword;
}
