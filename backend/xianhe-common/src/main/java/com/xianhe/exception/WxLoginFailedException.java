package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 微信登录时 code 换不到 openid
 */
public class WxLoginFailedException extends BaseException {

    public WxLoginFailedException() {
        super(MessageConstant.WX_LOGIN_FAILED);
    }
}
