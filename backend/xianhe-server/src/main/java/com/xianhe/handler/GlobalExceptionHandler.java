package com.xianhe.handler;

import com.xianhe.constant.MessageConstant;
import com.xianhe.exception.BaseException;
import com.xianhe.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 唯一键冲突信息形如：Duplicate entry 'zhangsan' for key 'staff.uk_staff_username'
    private static final Pattern DUPLICATE_ENTRY =
            Pattern.compile("Duplicate entry '(.+?)' for key");

    /**
     * 业务异常：消息本身就是给用户看的文案，直接返回
     * @param ex
     * @return
     */
    @ExceptionHandler(BaseException.class)
    public Result<Void> handleBaseException(BaseException ex) {
        log.warn("业务异常：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 唯一索引冲突：转成「xxx已存在」
     * @param ex
     * @return
     */
    @ExceptionHandler({DuplicateKeyException.class, SQLIntegrityConstraintViolationException.class})
    public Result<Void> handleDuplicateKey(Exception ex) {
        // 要处理的是 DuplicateKeyException 而不是 SQLIntegrityConstraintViolationException：
        // mybatis-spring 的 MyBatisExceptionTranslator 会把底层 SQL 异常翻译成 Spring 的
        // DataAccessException 体系，唯一键冲突对应 DuplicateKeyException。
        log.error("唯一索引冲突：{}", ex.getMessage());
        Matcher matcher = DUPLICATE_ENTRY.matcher(ex.getMessage());
        if (matcher.find()) {
            return Result.error(matcher.group(1) + MessageConstant.ALREADY_EXISTS);
        }
        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }

    /**
     * 上传文件超过体积上限
     * @param ex
     * @return
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        // 该异常在进入 Controller 之前就会抛出（由 MultipartResolver 触发），
        // 业务代码没有机会校验，所以必须在这里兜住并转成契约 2.3.1 定义的文案。
        log.warn("上传文件超出大小限制：{}", ex.getMessage());
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }

    /**
     * 兜底：未预期的异常记录完整堆栈，但只回传通用文案，
     * @param ex
     * @return
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        // 避免把 SQL、类名等内部信息暴露给客户端。
        log.error("系统异常", ex);
        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }
}
