package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 新增或修改组合箱时同一商品被提交多次
 */
public class BundleGoodsDuplicateException extends BaseException {

    public BundleGoodsDuplicateException() {
        super(MessageConstant.BUNDLE_GOODS_DUPLICATE);
    }
}
