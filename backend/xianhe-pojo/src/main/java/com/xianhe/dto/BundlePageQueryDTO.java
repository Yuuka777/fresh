package com.xianhe.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 组合箱分页查询入参（《接口文档》2.7.2），字段语义同 GoodsPageQueryDTO
 */
@Data
public class BundlePageQueryDTO implements Serializable {

    //组合箱名称，模糊匹配
    private String name;

    //所属组合分类 id
    private Long categoryId;

    //状态：0 停售 / 1 起售
    private Integer status;

    //页码，从 1 开始
    private Integer page;

    //每页条数
    private Integer pageSize;
}
