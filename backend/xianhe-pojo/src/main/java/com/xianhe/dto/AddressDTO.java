package com.xianhe.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 收货地址入参（《接口文档-用户端》8.2 新增 / 8.4 修改）
 */
@Data
public class AddressDTO implements Serializable {

    //主键，修改时必填；新增时不传
    private Long id;

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

    //详细地址
    private String detail;

    //标签，如「家」「公司」
    private String label;

    //是否默认地址：0 否 / 1 是；不传表示不改变该项
    private Integer isDefault;
}
