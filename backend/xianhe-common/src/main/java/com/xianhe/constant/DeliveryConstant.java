package com.xianhe.constant;

import java.math.BigDecimal;

/**
 * 配送相关常量
 */
public class DeliveryConstant {

    //运送费（元），与小程序端 common/config.js 的 DELIVERY_FEE 取同一个值
    public static final BigDecimal FEE = new BigDecimal("5.00");

    //配送时效：尽快送达
    public static final Integer STATUS_ASAP = 1;

    //配送时效：预约时段
    public static final Integer STATUS_SCHEDULED = 0;

    private DeliveryConstant() {
    }
}
