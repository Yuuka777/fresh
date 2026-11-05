package com.xianhe.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 会员统计（《接口文档》2.9.2）
 */
@Data
public class MemberReportVO implements Serializable {

    //日期序列，逗号分隔
    private String dateList;

    //每日新增会员数序列，逗号分隔
    private String newMemberList;

    //每日累计会员数序列，逗号分隔（截至当日 24 时）
    private String totalMemberList;
}
