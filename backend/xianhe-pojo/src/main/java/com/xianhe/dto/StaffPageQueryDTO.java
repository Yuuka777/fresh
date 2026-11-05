package com.xianhe.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 店员分页查询入参（《接口文档》2.1.4）
 */
@Data
public class StaffPageQueryDTO implements Serializable {

    //姓名，模糊匹配
    private String name;

    //用户名，模糊匹配
    private String username;

    //手机号，模糊匹配
    private String phone;

    //页码，从 1 开始
    private Integer page;

    //每页条数
    private Integer pageSize;
}
