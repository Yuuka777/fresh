package com.xianhe.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 分类分页查询入参（《接口文档》2.2.2）
 */
@Data
public class GoodsCategoryPageQueryDTO implements Serializable {

    //分类名称，模糊匹配，可不传
    private String name;

    //分类类型筛选，可不传
    private Integer type;

    //页码，从 1 开始
    private Integer page;

    //每页条数
    private Integer pageSize;
}
