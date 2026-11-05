package com.xianhe.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 订单统计（《接口文档》2.9.3）
 */
@Data
public class OrderReportVO implements Serializable {

    //日期序列，逗号分隔
    private String dateList;

    //每日订单总数序列，逗号分隔
    private String orderCountList;

    //每日有效订单数序列（status = 5），逗号分隔
    private String validOrderCountList;

    //区间内的订单总数
    private Integer totalOrderCount;

    //区间内的有效订单总数
    private Integer validOrderCount;

    //区间订单完成率＝有效订单总数 / 订单总数
    private Double orderCompletionRate;
}
