package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 删除店员时目标账号仍在启用
 */
public class StaffEnabledDeleteException extends BaseException {

    public StaffEnabledDeleteException() {
        super(MessageConstant.STAFF_ENABLED_DELETE_DENIED);
    }
}
