package com.xianhe.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 营业额统计（《接口文档》2.9.1）
 */
@Data
public class TurnoverReportVO implements Serializable {

    //日期序列，逗号分隔
    private String dateList;

    //每日营业额序列，逗号分隔；区间内没有订单的日期补 0
    private String turnoverList;
}
