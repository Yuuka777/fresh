package com.xianhe.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xianhe.constant.CategoryTypeConstant;
import com.xianhe.constant.MessageConstant;
import com.xianhe.constant.RedisKeyConstant;
import com.xianhe.constant.StatusConstant;
import com.xianhe.constant.StorageZoneConstant;
import com.xianhe.dto.BundleDTO;
import com.xianhe.dto.BundleGoodsDTO;
import com.xianhe.dto.BundlePageQueryDTO;
import com.xianhe.entity.Bundle;
import com.xianhe.entity.BundleGoods;
import com.xianhe.entity.Goods;
import com.xianhe.entity.GoodsCategory;
import com.xianhe.exception.BundleEnableFailedException;
import com.xianhe.exception.BundleGoodsCopiesException;
import com.xianhe.exception.BundleGoodsDuplicateException;
import com.xianhe.exception.BundleGoodsEmptyException;
import com.xianhe.exception.BundleNotFoundException;
import com.xianhe.exception.BundleOnSaleException;
import com.xianhe.exception.CategoryNotFoundException;
import com.xianhe.exception.CategoryTypeMismatchException;
import com.xianhe.exception.GoodsNotFoundException;
import com.xianhe.mapper.BundleGoodsMapper;
import com.xianhe.mapper.BundleMapper;
import com.xianhe.mapper.CartMapper;
import com.xianhe.mapper.GoodsCategoryMapper;
import com.xianhe.mapper.GoodsMapper;
import com.xianhe.result.PageResult;
import com.xianhe.service.BundleService;
import com.xianhe.vo.BundleGoodsVO;
import com.xianhe.vo.BundleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 组合箱业务实现
 */
@Service
@Slf4j
public class BundleServiceImpl implements BundleService {

    @Autowired
    private BundleMapper bundleMapper;

    @Autowired
    private BundleGoodsMapper bundleGoodsMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private GoodsCategoryMapper goodsCategoryMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public void add(BundleDTO bundleDTO) {
        assertCategoryUsable(bundleDTO.getCategoryId());
        Map<Long, Goods> goodsPool = loadGoodsPool(bundleDTO.getBundleGoods());

        Bundle bundle = new Bundle();
        BeanUtils.copyProperties(bundleDTO, bundle);
        // 带状态的数据新增后一律禁用（全局约定），需在列表中手动起售
        bundle.setStatus(StatusConstant.DISABLE);
        bundle.setStorageZone(deriveStorageZone(goodsPool.values()));

        // 名称重复由 bundle 表的唯一索引兜住
        bundleMapper.insert(bundle);
        int lines = saveBundleGoods(bundle.getId(), bundleDTO.getBundleGoods(), goodsPool);

        log.info("新增组合箱成功：id={}，name={}，categoryId={}，内含商品 {} 项，整箱温区={}",
                bundle.getId(), bundle.getName(), bundle.getCategoryId(), lines, bundle.getStorageZone());
    }

    @Override
    public PageResult pageQuery(BundlePageQueryDTO queryDTO) {
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize());
        List<BundleVO> list = bundleMapper.pageQuery(queryDTO);
        Page<BundleVO> page = (Page<BundleVO>) list;
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 删除组合箱
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        // 只有一道业务校验：起售中不能删（正在售卖的箱子直接消失会让用户下单失败）。
        // 与商品不同，组合箱被购物车引用不构成删除障碍 —— 组合箱本身就是销售单位，
        // 删掉后连带把购物车里的对应行清掉即可。
        if (ids == null || ids.isEmpty()) {
            log.info("批量删除组合箱：未提交任何 id，忽略");
            return;
        }

        if (bundleMapper.countOnSaleByIds(ids) > 0) {
            log.warn("组合箱删除被拒：{} 中存在起售中的组合箱", ids);
            throw new BundleOnSaleException();
        }

        int deleted = bundleMapper.deleteByIds(ids);
        bundleGoodsMapper.deleteByBundleIds(ids);
        cartMapper.deleteByBundleIds(ids);

        log.info("删除组合箱成功：请求 {} 个，实际删除 {} 个", ids.size(), deleted);
    }

    @Override
    public BundleVO getById(Long id) {
        BundleVO vo = bundleMapper.getVoById(id);
        if (vo == null) {
            throw new BundleNotFoundException();
        }
        vo.setBundleGoods(loadBundleGoodsVos(id));
        return vo;
    }

    @Override
    @Transactional
    public void update(BundleDTO bundleDTO) {
        Bundle existing = requireBundle(bundleDTO.getId());
        assertCategoryUsable(bundleDTO.getCategoryId());
        Map<Long, Goods> goodsPool = loadGoodsPool(bundleDTO.getBundleGoods());

        // 起售中的组合箱：改完之后必须仍然满足可售条件。
        // 少了这一步，就可以先起售、再把内含商品换成下架的，等于绕过 2.7.6 的校验。
        if (StatusConstant.ENABLE.equals(existing.getStatus())) {
            assertSellable(bundleDTO.getCategoryId(), bundleDTO.getBundleGoods().size(), goodsPool.values());
        }

        Bundle bundle = new Bundle();
        BeanUtils.copyProperties(bundleDTO, bundle);
        // 内含商品变了，整箱温区重新推导
        bundle.setStorageZone(deriveStorageZone(goodsPool.values()));
        bundleMapper.update(bundle);

        // 内含商品全量覆盖：先清空旧的，再插入本次提交的（契约 2.7.5）
        bundleGoodsMapper.deleteByBundleId(bundle.getId());
        int lines = saveBundleGoods(bundle.getId(), bundleDTO.getBundleGoods(), goodsPool);

        // 起售中的组合箱改了售价/内含商品，用户端缓存必须跟着失效；分类可能被改过，新旧两个键都要清
        if (StatusConstant.ENABLE.equals(existing.getStatus())) {
            clearCategoryCache(existing.getCategoryId());
            clearCategoryCache(bundleDTO.getCategoryId());
        }

        log.info("修改组合箱成功：id={}，name={}，内含商品 {} 项，整箱温区={}",
                bundle.getId(), bundle.getName(), lines, bundle.getStorageZone());
    }

    /**
     * 起售 / 停售
     * @param status
     * @param id
     */
    @Override
    @Transactional
    public void setStatus(Integer status, Long id) {
        // 起售要过三道校验（分类存在、分类启用、内含商品齐全且全部上架），共同保证
        // 「在售的组合箱一定是能完整交付的」；停售没有前置条件。
        Bundle bundle = requireBundle(id);

        if (StatusConstant.ENABLE.equals(status)) {
            List<BundleGoods> lines = bundleGoodsMapper.listByBundleId(id);
            assertSellable(bundle.getCategoryId(), lines.size(), loadGoodsOfBundle(lines));
        }

        Bundle update = new Bundle();
        update.setId(id);
        update.setStatus(status);
        bundleMapper.updateStatus(update);

        // 精准清除该组合箱所属分类下的用户端缓存，不清全量（契约 2.7.6）
        clearCategoryCache(bundle.getCategoryId());

        log.info("组合箱状态已变更：id={}，status={}，已清除该分类的用户端缓存", id, status);
    }

    //以下为内部方法

    private Bundle requireBundle(Long id) {
        Bundle bundle = bundleMapper.getById(id);
        if (bundle == null) {
            throw new BundleNotFoundException();
        }
        return bundle;
    }

    /**
     * 校验分类可用于挂组合箱：必须存在，且是组合分类（type = 2）
     * @param categoryId
     */
    private void assertCategoryUsable(Long categoryId) {
        GoodsCategory category = goodsCategoryMapper.getById(categoryId);
        if (category == null) {
            throw new CategoryNotFoundException();
        }
        if (!CategoryTypeConstant.BUNDLE.equals(category.getType())) {
            throw new CategoryTypeMismatchException();
        }
    }

    /**
     * 校验内含商品列表并回查商品，返回 商品 id -> 商品 的映射
     * @param items
     * @return
     */
    private Map<Long, Goods> loadGoodsPool(List<BundleGoodsDTO> items) {
        // 校验四件事：列表非空、每行份数为正、商品不重复、商品都真实存在。
        // 返回的映射供后续填充名称与单价、推导温区使用，避免重复查库。
        if (items == null || items.isEmpty()) {
            throw new BundleGoodsEmptyException();
        }

        List<Long> goodsIds = new ArrayList<>(items.size());
        Set<Long> distinct = new LinkedHashSet<>();
        for (BundleGoodsDTO item : items) {
            if (item.getGoodsId() == null) {
                throw new GoodsNotFoundException();
            }
            if (item.getCopies() == null || item.getCopies() < 1) {
                throw new BundleGoodsCopiesException();
            }
            if (!distinct.add(item.getGoodsId())) {
                throw new BundleGoodsDuplicateException();
            }
            goodsIds.add(item.getGoodsId());
        }

        List<Goods> goodsList = goodsMapper.listByIds(goodsIds);
        if (goodsList.size() != goodsIds.size()) {
            log.warn("组合箱保存被拒：内含商品 {} 中存在查不到的 id", goodsIds);
            throw new GoodsNotFoundException();
        }
        return goodsList.stream()
                .collect(Collectors.toMap(Goods::getId, goods -> goods, (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * 推导整箱温区：取内含商品的最高要求温区
     * @param goodsList
     * @return
     */
    private int deriveStorageZone(Collection<Goods> goodsList) {
        // 常温品与冷冻品混装时必须按冷冻运输，所以是取最大值而不是平均值。
        return goodsList.stream()
                .mapToInt(goods -> goods.getStorageZone() == null
                        ? StorageZoneConstant.NORMAL
                        : goods.getStorageZone())
                .max()
                .orElse(StorageZoneConstant.NORMAL);
    }

    /**
     * 写入内含商品，返回写入行数
     * @param bundleId
     * @param items
     * @param goodsPool
     * @return
     */
    private int saveBundleGoods(Long bundleId, List<BundleGoodsDTO> items, Map<Long, Goods> goodsPool) {
        // 名称与单价取自回查结果，前端提交的同名字段只作接收、不作依据。
        List<BundleGoods> entities = new ArrayList<>(items.size());
        for (BundleGoodsDTO item : items) {
            Goods goods = goodsPool.get(item.getGoodsId());
            BundleGoods entity = new BundleGoods();
            entity.setBundleId(bundleId);
            entity.setGoodsId(goods.getId());
            entity.setName(goods.getName());
            entity.setPrice(goods.getPrice());
            entity.setCopies(item.getCopies());
            entities.add(entity);
        }
        bundleGoodsMapper.insertBatch(entities);
        return entities.size();
    }

    /**
     * 起售（或保持起售）的前置校验
     * @param categoryId
     * @param expectedCount
     * @param goodsList
     */
    private void assertSellable(Long categoryId, int expectedCount, Collection<Goods> goodsList) {
        if (expectedCount == 0) {
            throw new BundleGoodsEmptyException();
        }

        GoodsCategory category = goodsCategoryMapper.getById(categoryId);
        if (category == null) {
            throw new CategoryNotFoundException();
        }
        if (StatusConstant.DISABLE.equals(category.getStatus())) {
            log.warn("组合箱起售被拒：分类 {} 处于禁用状态", categoryId);
            throw new BundleEnableFailedException(MessageConstant.BUNDLE_CATEGORY_DISABLED);
        }

        /*
          商品数与应含项数不一致，说明有内含商品已被删除。
          正常流程下删商品会被「商品关联组合箱，不能删除」拦住，
          这里兜的是「有人直接改库」之后的残留数据。
        */
        if (goodsList.size() != expectedCount) {
            log.warn("组合箱起售被拒：应含 {} 项，实际查到 {} 项，存在已被删除的内含商品",
                    expectedCount, goodsList.size());
            throw new BundleEnableFailedException();
        }

        if (goodsList.stream().anyMatch(goods -> !StatusConstant.ENABLE.equals(goods.getStatus()))) {
            log.warn("组合箱起售被拒：内含商品存在未上架项");
            throw new BundleEnableFailedException();
        }
    }

    /**
     * 按内含商品行查出商品；行为空时不查库，避免拼出非法的 IN ()
     * @param lines
     * @return
     */
    private List<Goods> loadGoodsOfBundle(List<BundleGoods> lines) {
        if (lines.isEmpty()) {
            return List.of();
        }
        List<Long> goodsIds = lines.stream().map(BundleGoods::getGoodsId).toList();
        return goodsMapper.listByIds(goodsIds);
    }

    /**
     * 清除某分类下的用户端组合箱列表缓存
     * @param categoryId
     */
    private void clearCategoryCache(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        stringRedisTemplate.delete(RedisKeyConstant.BUNDLE_BY_CATEGORY_PREFIX + categoryId);
    }

    /**
     * 读取内含商品，供详情接口返回
     * @param bundleId
     * @return
     */
    private List<BundleGoodsVO> loadBundleGoodsVos(Long bundleId) {
        return bundleGoodsMapper.listByBundleId(bundleId).stream().map(line -> {
            BundleGoodsVO vo = new BundleGoodsVO();
            vo.setId(line.getId());
            vo.setGoodsId(line.getGoodsId());
            vo.setName(line.getName());
            vo.setPrice(line.getPrice());
            vo.setCopies(line.getCopies());
            return vo;
        }).toList();
    }
}
