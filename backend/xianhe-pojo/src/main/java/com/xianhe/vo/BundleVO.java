package com.xianhe.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 组合箱出参（《接口文档》2.7.2、2.7.4）
 */
@Data
public class BundleVO implements Serializable {

    //组合箱 id
    private Long id;

    //组合箱名称
    private String name;

    //所属分类 id
    private Long categoryId;

    //分类名称（联表带出）
    private String categoryName;

    //售价
    private BigDecimal price;

    //图片 URL，可能为 null
    private String image;

    //描述
    private String description;

    //状态：0 停售 / 1 起售
    private Integer status;

    //整箱温区（由内含商品最高温区推导）
    private Integer storageZone;

    //更新时间，由全局 JSON 配置格式化为 yyyy-MM-dd HH:mm:ss
    private LocalDateTime updateTime;

    //内含商品列表，仅详情接口返回
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<BundleGoodsVO> bundleGoods;
}
