package com.xianhe.constant;

/**
 * 订单的业务常量：落库的取值，不是返回给前端的提示文案
 */
public class OrderConstant {

    //取消原因：用户主动取消（契约 9.5）
    public static final String CANCEL_REASON_USER = "用户取消";

    //取消原因：超过 15 分钟未支付，由定时任务自动取消（契约 2.8.9）
    public static final String CANCEL_REASON_TIMEOUT = "订单超时未支付，系统自动取消";

    private OrderConstant() {
    }
}
