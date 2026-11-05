package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 引用的分类不存在
 */
public class CategoryNotFoundException extends BaseException {

    public CategoryNotFoundException() {
        super(MessageConstant.CATEGORY_NOT_FOUND);
    }
}
