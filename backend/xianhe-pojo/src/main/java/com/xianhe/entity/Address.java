package com.xianhe.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 收货地址（表 address）
 */
@Data
public class Address implements Serializable {

    //主键
    private Long id;

    //所属会员 id（服务端从令牌解析填充，前端不传）
    private Long memberId;

    //收货人
    private String consignee;

    //性别：男 / 女
    private String sex;

    //手机号，11 位
    private String phone;

    //省级区划编号
    private String provinceCode;

    //省级名称
    private String provinceName;

    //市级区划编号
    private String cityCode;

    //市级名称
    private String cityName;

    //区级区划编号
    private String districtCode;

    //区级名称
    private String districtName;

    //详细地址，街道楼栋门牌号
    private String detail;

    //标签，如「家」「公司」
    private String label;

    //是否默认地址：0 否 / 1 是，同一会员至多一条为 1
    private Integer isDefault;
}
