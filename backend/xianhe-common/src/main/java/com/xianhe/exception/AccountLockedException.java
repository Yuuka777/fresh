package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 账号被锁定
 */
public class AccountLockedException extends BaseException {

    public AccountLockedException() {
        super(MessageConstant.ACCOUNT_LOCKED);
    }
}
