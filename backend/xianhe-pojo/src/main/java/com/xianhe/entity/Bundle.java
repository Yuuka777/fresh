package com.xianhe.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 组合箱（表 bundle）
 */
@Data
public class Bundle extends BaseEntity implements Serializable {

    //主键
    private Long id;

    //所属组合分类 id，指向 goods_category 中 type = 2 的记录
    private Long categoryId;

    //组合箱名称，全局唯一（库中 uk_bundle_name 唯一索引）
    private String name;

    //售价
    private BigDecimal price;

    //状态：0 停售 / 1 起售
    private Integer status;

    //描述
    private String description;

    //图片 URL，可为空
    private String image;

    // 整箱温区：1 常温 / 2 冷藏 / 3 冷冻
    private Integer storageZone;
}
