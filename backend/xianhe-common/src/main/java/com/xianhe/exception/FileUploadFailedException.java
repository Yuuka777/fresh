package com.xianhe.exception;

import com.xianhe.constant.MessageConstant;

/**
 * 文件上传失败
 */
public class FileUploadFailedException extends BaseException {

    public FileUploadFailedException() {
        super(MessageConstant.UPLOAD_FAILED);
    }
}
