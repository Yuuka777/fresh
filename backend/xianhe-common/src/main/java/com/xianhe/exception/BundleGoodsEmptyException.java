package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 新增或修改组合箱时未提交任何内含商品
 */
public class BundleGoodsEmptyException extends BaseException {

    public BundleGoodsEmptyException() {
        super(MessageConstant.BUNDLE_GOODS_EMPTY);
    }
}
