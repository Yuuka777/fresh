package com.xianhe.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 新增 / 修改商品入参（《接口文档》2.6.1、2.6.5）
 */
@Data
public class GoodsDTO implements Serializable {

    //商品 id，修改时必填
    private Long id;

    //商品名称，≤32 字符，全局唯一
    private String name;

    //所属商品分类 id（type = 1）
    private Long categoryId;

    //售价
    private BigDecimal price;

    //图片 URL，可为空
    private String image;

    //描述，≤255 字符
    private String description;

    //产地，≤64 字符
    private String origin;

    //保质期文案，如「冷藏 5 天」
    private String shelfLife;

    //存储温区：1 常温 / 2 冷藏 / 3 冷冻
    private Integer storageZone;

    //规格列表，可为空；修改时全量覆盖
    private List<GoodsSpecDTO> specs;
}
