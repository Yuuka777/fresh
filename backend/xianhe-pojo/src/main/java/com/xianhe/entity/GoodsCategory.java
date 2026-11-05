package com.xianhe.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 商品分类（表 goods_category）
 */
@Data
public class GoodsCategory extends BaseEntity implements Serializable {

    //主键
    private Long id;

    //分类类型：1 商品分类 / 2 组合分类
    private Integer type;

    //分类名称，全局唯一
    private String name;

    //排序值，越小越靠前
    private Integer sort;

    //状态：0 禁用 / 1 启用
    private Integer status;
}
