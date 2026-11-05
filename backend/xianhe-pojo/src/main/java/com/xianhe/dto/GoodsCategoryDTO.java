package com.xianhe.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 新增 / 修改分类入参（《接口文档》2.2.1、2.2.4）
 */
@Data
public class GoodsCategoryDTO implements Serializable {

    //分类 id，修改时必填
    private Long id;

    //分类类型：1 商品分类 / 2 组合分类，新增时必填
    private Integer type;

    //分类名称
    private String name;

    //排序值，越小越靠前
    private Integer sort;
}
