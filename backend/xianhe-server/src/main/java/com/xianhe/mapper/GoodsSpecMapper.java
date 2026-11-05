package com.xianhe.mapper;

import com.xianhe.entity.GoodsSpec;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品规格持久层（表 goods_spec）
 */
public interface GoodsSpecMapper {

    /**
     * 批量插入规格
     * @param specs
     */
    void insertBatch(@Param("specs") List<GoodsSpec> specs);

    /**
     * 查询某商品的规格，按 id 升序（即录入顺序，前端按此顺序渲染候选值）
     * @param goodsId
     * @return
     */
    List<GoodsSpec> listByGoodsId(@Param("goodsId") Long goodsId);

    /**
     * 删除某商品的全部规格，修改商品时「先删后插」实现全量覆盖（契约 2.6.5）
     * @param goodsId
     */
    void deleteByGoodsId(@Param("goodsId") Long goodsId);

    /**
     * 按商品批量删除规格，删除商品时随之清理
     * @param goodsIds
     */
    void deleteByGoodsIds(@Param("goodsIds") List<Long> goodsIds);

    /**
     * 按商品批量查规格，用户端列表一次取全（避免逐个商品再查一次库）
     * @param goodsIds
     * @return
     */
    List<GoodsSpec> listByGoodsIds(@Param("goodsIds") List<Long> goodsIds);
}
