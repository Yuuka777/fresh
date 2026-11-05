package com.xianhe.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车行（《接口文档-用户端》7.2）
 * <p>
 * 名称、图片、单价都按**现值**回查覆盖（不是加购时的快照），
 * 并带上可结算状态，供前端标注「已下架」；库里仍保留加购时的快照。
 */
@Data
public class CartVO implements Serializable {

    //购物车行 id
    private Long id;

    //商品 / 组合箱名称，现值
    private String name;

    //图片，现值，可为 null
    private String image;

    //商品 id
    private Long goodsId;

    //组合箱 id
    private Long bundleId;

    //规格文本，加购时的快照（商品规格改动不影响已加购的行的判定）
    private String specText;

    //数量
    private Integer number;

    //小计 = 现值单价 × 数量
    private BigDecimal amount;

    //加入购物车的时间
    private LocalDateTime createTime;

    //可结算状态：1 在售（商品上架 / 组合箱起售）/ 0 已下架或已停售
    private Integer status;

    //现值单价与加购时的快照单价是否不同
    private Boolean priceChanged;
}
