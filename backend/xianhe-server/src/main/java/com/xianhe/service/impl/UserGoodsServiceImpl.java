package com.xianhe.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xianhe.cache.CategoryListCache;
import com.xianhe.constant.RedisKeyConstant;
import com.xianhe.entity.Goods;
import com.xianhe.entity.GoodsSpec;
import com.xianhe.mapper.GoodsMapper;
import com.xianhe.mapper.GoodsSpecMapper;
import com.xianhe.service.UserGoodsService;
import com.xianhe.utils.JsonUtil;
import com.xianhe.vo.GoodsSpecVO;
import com.xianhe.vo.GoodsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户端商品浏览业务实现
 */
@Service
@Slf4j
public class UserGoodsServiceImpl implements UserGoodsService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private GoodsSpecMapper goodsSpecMapper;

    @Autowired
    private CategoryListCache categoryListCache;

    @Override
    public List<GoodsVO> listByCategoryId(Long categoryId) {
        // 按分类查是全店最热的读路径：缓存命中直接返回，未命中由缓存器加锁回源
        return categoryListCache.getOrLoad(RedisKeyConstant.GOODS_BY_CATEGORY_PREFIX, categoryId,
                new TypeReference<List<GoodsVO>>() {
                },
                () -> assemble(goodsMapper.listOnSaleByCategoryId(categoryId)));
    }

    @Override
    public List<GoodsVO> searchByName(String name) {
        String keyword = name == null ? "" : name.trim();
        if (keyword.isEmpty()) {
            // 契约 5.2：关键词为空时返回空数组，不能退化成「返回全量」
            return List.of();
        }
        // 搜索走不走缓存取决于关键词分布，这里直接查库，避免缓存被长尾关键词撑满
        return assemble(goodsMapper.searchOnSaleByName(keyword));
    }

    // 把商品实体组装成出参，并把规格一次取全（逐个商品再查一次库会变成 N+1）
    private List<GoodsVO> assemble(List<Goods> goodsList) {
        if (goodsList.isEmpty()) {
            return List.of();
        }
        List<Long> goodsIds = goodsList.stream().map(Goods::getId).toList();
        Map<Long, List<GoodsSpecVO>> specMap = goodsSpecMapper.listByGoodsIds(goodsIds).stream()
                .collect(Collectors.groupingBy(GoodsSpec::getGoodsId, LinkedHashMap::new,
                        Collectors.mapping(this::toSpecVO, Collectors.toList())));

        return goodsList.stream().map(goods -> toVO(goods, specMap.get(goods.getId()))).toList();
    }

    private GoodsVO toVO(Goods goods, List<GoodsSpecVO> specs) {
        GoodsVO vo = new GoodsVO();
        vo.setId(goods.getId());
        vo.setName(goods.getName());
        vo.setCategoryId(goods.getCategoryId());
        vo.setPrice(goods.getPrice());
        vo.setImage(goods.getImage());
        vo.setDescription(goods.getDescription());
        vo.setStatus(goods.getStatus());
        vo.setOrigin(goods.getOrigin());
        vo.setShelfLife(goods.getShelfLife());
        vo.setStorageZone(goods.getStorageZone());
        // 无规格商品这里为 null，前端据它决定「直接加购」还是「先选规格」
        vo.setSpecs(specs);
        return vo;
    }

    private GoodsSpecVO toSpecVO(GoodsSpec spec) {
        GoodsSpecVO vo = new GoodsSpecVO();
        vo.setId(spec.getId());
        vo.setName(spec.getName());
        vo.setValue(JsonUtil.toStringArray(spec.getValue()));
        return vo;
    }
}
