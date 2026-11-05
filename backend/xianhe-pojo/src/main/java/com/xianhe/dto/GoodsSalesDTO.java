package com.xianhe.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 商品销量（《接口文档》2.9.4 销量 TOP10 的数组元素）
 */
@Data
public class GoodsSalesDTO implements Serializable {

    //商品名称（订单明细里的快照名）
    private String name;

    //销量：已完成订单中该商品的份数合计
    private Integer number;
}
