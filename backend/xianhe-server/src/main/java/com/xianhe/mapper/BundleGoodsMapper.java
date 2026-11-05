package com.xianhe.mapper;

import com.xianhe.entity.BundleGoods;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 组合箱内含商品持久层（表 bundle_goods）
 */
public interface BundleGoodsMapper {

    /**
     * 批量插入内含商品
     * @param bundleGoods
     */
    void insertBatch(@Param("bundleGoods") List<BundleGoods> bundleGoods);

    /**
     * 查询某组合箱的内含商品，按 id 升序（录入顺序）
     * @param bundleId
     * @return
     */
    List<BundleGoods> listByBundleId(@Param("bundleId") Long bundleId);

    /**
     * 删除某组合箱的全部内含商品，修改组合箱时「先删后插」实现全量覆盖（契约 2.7.5）
     * @param bundleId
     */
    void deleteByBundleId(@Param("bundleId") Long bundleId);

    /**
     * 按组合箱批量删除内含商品，删除组合箱时随之清理
     * @param bundleIds
     */
    void deleteByBundleIds(@Param("bundleIds") List<Long> bundleIds);

    /**
     * 统计这批商品被多少个组合箱引用（不论组合箱状态），用于删除商品前的校验
     * @param goodsIds
     * @return
     */
    int countByGoodsIds(@Param("goodsIds") List<Long> goodsIds);

    /**
     * 统计这批商品被多少个**起售中**的组合箱引用，用于下架商品前的校验
     * @param goodsIds
     * @return
     */
    int countOnSaleByGoodsIds(@Param("goodsIds") List<Long> goodsIds);

    /**
     * 按组合箱批量查内含商品，用户端列表一次取全
     * @param bundleIds
     * @return
     */
    List<BundleGoods> listByBundleIds(@Param("bundleIds") List<Long> bundleIds);
}
