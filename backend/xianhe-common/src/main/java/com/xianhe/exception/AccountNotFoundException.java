package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 账号不存在
 */
public class AccountNotFoundException extends BaseException {

    public AccountNotFoundException() {
        super(MessageConstant.ACCOUNT_NOT_FOUND);
    }
}
