package com.xianhe.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 商品规格入参（《接口文档》2.6.1 的 specs 元素）
 */
@Data
public class GoodsSpecDTO implements Serializable {

    //规格类型名称，如「净重」
    private String name;

    //候选值数组，如 ["约500g","约1kg"]
    private List<String> value;
}
