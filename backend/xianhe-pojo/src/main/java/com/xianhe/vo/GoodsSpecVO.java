package com.xianhe.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 商品规格出参（《接口文档》2.6.4 的 specs 元素）
 */
@Data
public class GoodsSpecVO implements Serializable {

    //规格 id
    private Long id;

    //规格类型名称，如「净重」
    private String name;

    //候选值数组
    private List<String> value;
}
