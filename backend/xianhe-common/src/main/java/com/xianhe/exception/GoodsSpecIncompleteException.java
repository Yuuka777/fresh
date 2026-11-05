package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 商品规格缺少名称或候选值
 */
public class GoodsSpecIncompleteException extends BaseException {

    public GoodsSpecIncompleteException() {
        super(MessageConstant.GOODS_SPEC_INCOMPLETE);
    }
}
