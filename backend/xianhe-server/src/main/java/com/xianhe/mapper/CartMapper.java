package com.xianhe.mapper;

import com.xianhe.entity.Cart;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 购物车持久层（表 {@code cart}）
 */
public interface CartMapper {

    //—— 用户端 ——

    /**
     * 查某会员的购物车，按加入顺序
     * @param memberId
     * @return
     */
    List<Cart> listByMemberId(Long memberId);

    /**
     * 按「会员 + 商品/组合箱 + 规格文本」查同款记录
     * @param memberId
     * @param goodsId
     * @param bundleId
     * @param specText
     * @return
     */
    Cart getBySameLine(@Param("memberId") Long memberId, @Param("goodsId") Long goodsId,
                       @Param("bundleId") Long bundleId, @Param("specText") String specText);

    /**
     * 新增一条购物车记录，回填自增主键
     * @param cart
     */
    void insert(Cart cart);

    /**
     * 按主键改数量与小计（加、减都走这一条）
     * @param cart
     */
    void updateNumberById(Cart cart);

    /**
     * 按主键删除
     * @param id
     */
    void deleteById(Long id);

    /**
     * 按主键删除，且限定归属：返回影响行数，0 表示这行不在该会员的车里
     * @param id
     * @param memberId
     * @return
     */
    int deleteByIdAndMember(@Param("id") Long id, @Param("memberId") Long memberId);

    /**
     * 清空某会员的购物车
     * @param memberId
     */
    void deleteByMemberId(Long memberId);

    //—— 管理端：删除商品 / 组合箱时的连带清理 ——

    /**
     * 删除引用了这批商品的购物车行
     * @param goodsIds
     */
    void deleteByGoodsIds(@Param("goodsIds") List<Long> goodsIds);

    /**
     * 删除引用了这批组合箱的购物车行
     * @param bundleIds
     */
    void deleteByBundleIds(@Param("bundleIds") List<Long> bundleIds);
}
