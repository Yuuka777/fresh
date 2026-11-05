package com.xianhe.aspect;

import com.xianhe.annotation.AutoFill;
import com.xianhe.constant.AutoFillConstant;
import com.xianhe.context.BaseContext;
import com.xianhe.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 公共字段自动填充切面
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    /**
     * 切入点：com.xianhe.mapper 包下所有标注了 @AutoFill 的方法
     */
    @Pointcut("execution(* com.xianhe.mapper.*.*(..)) && @annotation(com.xianhe.annotation.AutoFill)")
    public void autoFillPointcut() {
    }

    @Before("autoFillPointcut()")
    public void autoFill(JoinPoint joinPoint) {
        // 取当前方法上的注解，判断是新增还是修改
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        OperationType operationType = autoFill.value();

        // 约定：被拦截方法的第一个参数就是要落库的实体
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }
        Object entity = args[0];
        if (entity == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        if (operationType == OperationType.INSERT) {
            invoke(entity, AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class, now);
            invoke(entity, AutoFillConstant.SET_CREATE_USER, Long.class, currentId);
            invoke(entity, AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class, now);
            invoke(entity, AutoFillConstant.SET_UPDATE_USER, Long.class, currentId);
        } else if (operationType == OperationType.UPDATE) {
            invoke(entity, AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class, now);
            invoke(entity, AutoFillConstant.SET_UPDATE_USER, Long.class, currentId);
        }
    }

    /**
     * 反射调用实体 setter
     * @param entity
     * @param methodName
     * @param paramType
     * @param value
     */
    private void invoke(Object entity, String methodName, Class<?> paramType, Object value) {
        try {
            Method method = entity.getClass().getMethod(methodName, paramType);
            method.invoke(entity, value);
        } catch (NoSuchMethodException e) {
            // 实体没有该字段（例如只继承了一半），跳过而不是让整条业务失败
            log.debug("实体 {} 缺少方法 {}，跳过自动填充", entity.getClass().getSimpleName(), methodName);
        } catch (Exception e) {
            throw new RuntimeException("公共字段自动填充失败：" + methodName, e);
        }
    }
}
