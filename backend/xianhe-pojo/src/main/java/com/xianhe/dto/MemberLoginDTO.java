package com.xianhe.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 微信登录入参
 */
@Data
public class MemberLoginDTO implements Serializable {

    //小程序 wx.login / uni.login 返回的临时登录凭证
    private String code;
}
