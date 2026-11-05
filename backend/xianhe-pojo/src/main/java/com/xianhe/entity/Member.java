package com.xianhe.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员
 */
@Data
public class Member implements Serializable {

    //主键
    private Long id;

    //微信用户唯一标识（登录凭证），全局唯一
    private String openid;

    //昵称
    private String name;

    //手机号
    private String phone;

    //性别
    private String sex;

    //头像 URL
    private String avatar;

    //注册时间（本表不记录操作人，因此不继承 BaseEntity）
    private LocalDateTime createTime;
}
