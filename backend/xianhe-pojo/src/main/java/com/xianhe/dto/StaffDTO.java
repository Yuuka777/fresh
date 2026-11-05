package com.xianhe.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 新增 / 编辑店员入参（《接口文档》2.1.3、2.1.7）
 */
@Data
public class StaffDTO implements Serializable {

    //店员 id，编辑时必填
    private Long id;

    //姓名
    private String name;

    //登录用户名，全局唯一
    private String username;

    //手机号
    private String phone;

    //性别：男 / 女
    private String sex;

    //身份证号
    private String idNumber;
}
