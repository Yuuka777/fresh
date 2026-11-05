package com.xianhe.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;

/**
 * 店员（表 staff）
 */
@Data
public class Staff extends BaseEntity implements Serializable {

    //主键
    private Long id;

    //姓名
    private String name;

    //登录用户名，全局唯一
    private String username;

    // 密码（MD5 密文）
    @JsonIgnore
    private String password;

    //手机号
    private String phone;

    //性别：男 / 女
    private String sex;

    //身份证号
    private String idNumber;

    //状态：0 禁用 / 1 启用
    private Integer status;
}
