package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 原密码错误
 */
public class PasswordEditFailedException extends BaseException {

    public PasswordEditFailedException() {
        super(MessageConstant.PASSWORD_EDIT_FAILED);
    }
}
