package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 密码错误
 */
public class PasswordErrorException extends BaseException {

    public PasswordErrorException() {
        super(MessageConstant.PASSWORD_ERROR);
    }
}
