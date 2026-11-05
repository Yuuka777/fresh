package com.xianhe.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 商品分页查询入参（《接口文档》2.6.2）
 */
@Data
public class GoodsPageQueryDTO implements Serializable {

    //商品名称，模糊匹配
    private String name;

    //所属分类 id
    private Long categoryId;

    //上架状态：0 下架 / 1 上架
    private Integer status;

    //页码，从 1 开始
    private Integer page;

    //每页条数
    private Integer pageSize;
}
