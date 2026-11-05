package com.xianhe.context;

/**
 * 当前登录人上下文
 */
public class BaseContext {

    private static final ThreadLocal<Long> CURRENT_ID = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        CURRENT_ID.set(id);
    }

    public static Long getCurrentId() {
        return CURRENT_ID.get();
    }

    public static void removeCurrentId() {
        CURRENT_ID.remove();
    }
}
