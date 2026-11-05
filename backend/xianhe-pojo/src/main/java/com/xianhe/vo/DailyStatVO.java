package com.xianhe.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 按天聚合的统计结果，日报表的查询载体（不是接口返回结构）
 */
@Data
public class DailyStatVO implements Serializable {

    //日期，格式 yyyy-MM-dd
    private String statDate;

    //按天计数的场景：订单数 / 有效订单数 / 新增会员数
    private Integer count;

    //按天求和的场景：当日营业额
    private BigDecimal amount;
}
