package com.xianhe.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 组合箱内含商品（表 bundle_goods）
 */
@Data
public class BundleGoods implements Serializable {

    //主键
    private Long id;

    //所属组合箱 id
    private Long bundleId;

    //商品 id
    private Long goodsId;

    //商品名称（冗余快照，服务端回填）
    private String name;

    //商品单价（冗余快照，服务端回填）
    private BigDecimal price;

    //份数
    private Integer copies;
}
