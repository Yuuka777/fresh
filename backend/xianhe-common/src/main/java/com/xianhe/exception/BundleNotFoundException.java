package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 按 id 查询组合箱时查不到记录
 */
public class BundleNotFoundException extends BaseException {

    public BundleNotFoundException() {
        super(MessageConstant.BUNDLE_NOT_FOUND);
    }
}
