package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 分类类型与业务对象不匹配
 */
public class CategoryTypeMismatchException extends BaseException {

    public CategoryTypeMismatchException() {
        super(MessageConstant.CATEGORY_TYPE_MISMATCH);
    }
}
