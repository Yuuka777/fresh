package com.xianhe.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户下单入参（《接口文档-用户端》9.1）
 * <p>
 * 只声明服务端真正会用到的字段：收货人 / 手机号 / 完整地址按 {@code addressBookId} 回查，
 * 金额、明细、会员昵称由服务端自己算或查。前端即使把这些一起提交，也会被当成未知字段丢掉。
 */
@Data
public class OrderSubmitDTO implements Serializable {

    //收货地址 id
    private Long addressBookId;

    //支付方式：1 微信，不传按 1
    private Integer payMethod;

    //备注
    private String remark;

    //预计送达时间
    private LocalDateTime estimatedDeliveryTime;

    //配送时效：1 尽快送达 / 0 预约时段
    private Integer deliveryStatus;
}
