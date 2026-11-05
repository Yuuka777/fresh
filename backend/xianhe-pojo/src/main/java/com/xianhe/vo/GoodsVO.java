package com.xianhe.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品出参（《接口文档》2.6.2、2.6.4）
 */
@Data
public class GoodsVO implements Serializable {

    //商品 id
    private Long id;

    //商品名称
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

    //状态：0 下架 / 1 上架
    private Integer status;

    //产地
    private String origin;

    //保质期
    private String shelfLife;

    //存储温区
    private Integer storageZone;

    //更新时间，由全局 JSON 配置格式化为 yyyy-MM-dd HH:mm:ss
    private LocalDateTime updateTime;

    //规格列表，仅详情接口返回
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<GoodsSpecVO> specs;
}
