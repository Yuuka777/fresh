package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 操作人试图删除自己的账号
 */
public class SelfDeleteException extends BaseException {

    public SelfDeleteException() {
        super(MessageConstant.SELF_DELETE_DENIED);
    }
}
