package com.xianhe.constant;

/**
 * 支付状态常量（order_main.pay_status）
 */
public class PayStatusConstant {

    //未支付
    public static final Integer UN_PAID = 0;

    //已支付
    public static final Integer PAID = 1;

    //已退款
    public static final Integer REFUNDED = 2;

    private PayStatusConstant() {
    }
}
