package com.xianhe.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 店员登录响应（《接口文档》2.1.1）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StaffLoginVO implements Serializable {

    //店员 id
    private Long id;

    //用户名
    private String userName;

    //姓名
    private String name;

    //JWT 令牌，前端写入 Cookie key = token
    private String token;
}
