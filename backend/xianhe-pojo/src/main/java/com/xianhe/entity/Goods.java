package com.xianhe.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品（表 goods）
 */
@Data
public class Goods extends BaseEntity implements Serializable {

    //主键
    private Long id;

    //商品名称，全局唯一（库中 uk_goods_name 唯一索引）
    private String name;

    //所属商品分类 id，指向 goods_category 中 type = 1 的记录
    private Long categoryId;

    //售价
    private BigDecimal price;

    //图片 URL，可为空；为空时前端用「商品名首字 + 温区配色」占位
    private String image;

    //描述
    private String description;

    //状态：0 下架 / 1 上架
    private Integer status;

    //产地
    private String origin;

    //保质期文案，如「冷藏 5 天」
    private String shelfLife;

    //存储温区：1 常温 / 2 冷藏 / 3 冷冻
    private Integer storageZone;
}
