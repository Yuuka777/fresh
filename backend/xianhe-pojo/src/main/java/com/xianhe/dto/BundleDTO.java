package com.xianhe.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 新增 / 修改组合箱入参（《接口文档》2.7.1、2.7.5）
 */
@Data
public class BundleDTO implements Serializable {

    //组合箱 id，修改时必填
    private Long id;

    //组合箱名称，≤32 字符，全局唯一
    private String name;

    //所属组合分类 id（type = 2）
    private Long categoryId;

    //售价
    private BigDecimal price;

    //描述，≤255 字符
    private String description;

    //图片 URL，可为空
    private String image;

    //内含商品列表，不可为空；修改时全量覆盖
    private List<BundleGoodsDTO> bundleGoods;
}
