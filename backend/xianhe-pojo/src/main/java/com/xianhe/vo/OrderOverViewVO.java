package com.xianhe.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 订单总览（《接口文档》2.5.2）
 */
@Data
public class OrderOverViewVO implements Serializable {

    //待接单数量（不限时间：与订单页「待接单」页签口径一致）
    private Integer waitingOrders;

    //待派送数量（不限时间：与订单页「待派送」页签口径一致）
    private Integer deliveredOrders;

    //今日已完成数量
    private Integer completedOrders;

    //今日已取消数量
    private Integer cancelledOrders;

    //今日全部订单数
    private Integer allOrders;
}
