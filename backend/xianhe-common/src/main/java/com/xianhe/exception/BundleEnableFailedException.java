package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 组合箱起售失败
 */
public class BundleEnableFailedException extends BaseException {

    public BundleEnableFailedException() {
        super(MessageConstant.BUNDLE_ENABLE_FAILED);
    }

    public BundleEnableFailedException(String msg) {
        super(msg);
    }
}
