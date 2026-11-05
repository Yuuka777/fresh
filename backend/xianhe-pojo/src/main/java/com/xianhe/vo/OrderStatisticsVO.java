package com.xianhe.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 各状态订单数量统计（《接口文档》2.8.2）
 */
@Data
public class OrderStatisticsVO implements Serializable {

    //待付款数量（status = 1）
    private Integer toBePaid;

    //待接单数量（status = 2）
    private Integer toBeConfirmed;

    //待派送数量（status = 3）
    private Integer confirmed;

    //派送中数量（status = 4）
    private Integer deliveryInProgress;
}
