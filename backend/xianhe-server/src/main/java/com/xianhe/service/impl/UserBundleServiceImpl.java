package com.xianhe.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xianhe.cache.CategoryListCache;
import com.xianhe.constant.RedisKeyConstant;
import com.xianhe.entity.Bundle;
import com.xianhe.entity.BundleGoods;
import com.xianhe.mapper.BundleGoodsMapper;
import com.xianhe.mapper.BundleMapper;
import com.xianhe.service.UserBundleService;
import com.xianhe.vo.BundleGoodsVO;
import com.xianhe.vo.BundleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户端组合箱浏览业务实现
 */
@Service
@Slf4j
public class UserBundleServiceImpl implements UserBundleService {

    @Autowired
    private BundleMapper bundleMapper;

    @Autowired
    private BundleGoodsMapper bundleGoodsMapper;

    @Autowired
    private CategoryListCache categoryListCache;

    @Override
    public List<BundleVO> listByCategoryId(Long categoryId) {
        return categoryListCache.getOrLoad(RedisKeyConstant.BUNDLE_BY_CATEGORY_PREFIX, categoryId,
                new TypeReference<List<BundleVO>>() {
                },
                () -> assemble(bundleMapper.listOnSaleByCategoryId(categoryId)));
    }

    @Override
    public List<BundleGoods> listGoodsByBundleId(Long bundleId) {
        // 契约 6.2 是详情类查询：数据量小、只在用户点开某个箱子时触发，不额外加缓存
        return bundleGoodsMapper.listByBundleId(bundleId);
    }

    // 列表要带上内含商品（前端在卡片上直接展示「含哪些菜」），一次取全避免 N+1
    private List<BundleVO> assemble(List<Bundle> bundleList) {
        if (bundleList.isEmpty()) {
            return List.of();
        }
        List<Long> bundleIds = bundleList.stream().map(Bundle::getId).toList();
        Map<Long, List<BundleGoodsVO>> lineMap = bundleGoodsMapper.listByBundleIds(bundleIds).stream()
                .collect(Collectors.groupingBy(BundleGoods::getBundleId, LinkedHashMap::new,
                        Collectors.mapping(this::toLineVO, Collectors.toList())));

        return bundleList.stream().map(bundle -> {
            BundleVO vo = new BundleVO();
            vo.setId(bundle.getId());
            vo.setName(bundle.getName());
            vo.setCategoryId(bundle.getCategoryId());
            vo.setPrice(bundle.getPrice());
            vo.setImage(bundle.getImage());
            vo.setDescription(bundle.getDescription());
            vo.setStatus(bundle.getStatus());
            vo.setStorageZone(bundle.getStorageZone());
            vo.setBundleGoods(lineMap.get(bundle.getId()));
            return vo;
        }).toList();
    }

    private BundleGoodsVO toLineVO(BundleGoods line) {
        BundleGoodsVO vo = new BundleGoodsVO();
        vo.setId(line.getId());
        vo.setGoodsId(line.getGoodsId());
        vo.setName(line.getName());
        vo.setPrice(line.getPrice());
        vo.setCopies(line.getCopies());
        return vo;
    }
}
