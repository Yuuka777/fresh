package com.xianhe.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 今日运营数据（《接口文档》2.5.1）
 */
@Data
public class BusinessDataVO implements Serializable {

    //今日营业额（已完成订单金额合计）
    private Double turnover;

    //今日有效订单数（已完成）
    private Integer validOrderCount;

    //订单完成率＝有效订单数 / 总订单数
    private Double orderCompletionRate;

    //平均客单价＝营业额 / 有效订单数
    private Double unitPrice;

    //今日新增会员数
    private Integer newMembers;
}
