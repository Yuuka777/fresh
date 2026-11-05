package com.xianhe.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单分页查询条件（《接口文档-用户端》9.3、《接口文档》2.8.1）
 * <p>
 * 用户端只用 {@code memberId} + {@code status} + 分页；管理端的条件搜索还会用到订单号、手机号与时间区间，
 * 因此共用一个 DTO。{@code memberId} 由服务端从令牌取，不接受前端传入。
 */
@Data
public class OrderPageQueryDTO implements Serializable {

    //会员 id，用户端按当前登录会员过滤，管理端可留空表示不限制
    private Long memberId;

    //订单号，模糊匹配
    private String number;

    //下单手机号，模糊匹配
    private String phone;

    //订单状态
    private Integer status;

    //下单开始时间
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;

    //下单结束时间
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    //页码
    private Integer page;

    //每页条数
    private Integer pageSize;
}
