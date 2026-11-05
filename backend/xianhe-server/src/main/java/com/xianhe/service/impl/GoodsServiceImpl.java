package com.xianhe.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xianhe.constant.CategoryTypeConstant;
import com.xianhe.constant.RedisKeyConstant;
import com.xianhe.constant.StatusConstant;
import com.xianhe.dto.GoodsDTO;
import com.xianhe.dto.GoodsPageQueryDTO;
import com.xianhe.dto.GoodsSpecDTO;
import com.xianhe.entity.Goods;
import com.xianhe.entity.GoodsCategory;
import com.xianhe.entity.GoodsSpec;
import com.xianhe.exception.CategoryNotFoundException;
import com.xianhe.exception.CategoryTypeMismatchException;
import com.xianhe.exception.GoodsDisableFailedException;
import com.xianhe.exception.GoodsEnableFailedException;
import com.xianhe.exception.GoodsNotFoundException;
import com.xianhe.exception.GoodsOnSaleException;
import com.xianhe.exception.GoodsRelatedByBundleException;
import com.xianhe.exception.GoodsSpecIncompleteException;
import com.xianhe.mapper.BundleGoodsMapper;
import com.xianhe.mapper.CartMapper;
import com.xianhe.mapper.GoodsCategoryMapper;
import com.xianhe.mapper.GoodsMapper;
import com.xianhe.mapper.GoodsSpecMapper;
import com.xianhe.result.PageResult;
import com.xianhe.service.GoodsService;
import com.xianhe.utils.JsonUtil;
import com.xianhe.vo.GoodsSpecVO;
import com.xianhe.vo.GoodsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品业务实现
 */
@Service
@Slf4j
public class GoodsServiceImpl implements GoodsService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private GoodsSpecMapper goodsSpecMapper;

    @Autowired
    private GoodsCategoryMapper goodsCategoryMapper;

    @Autowired
    private BundleGoodsMapper bundleGoodsMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public void add(GoodsDTO goodsDTO) {
        assertCategoryUsable(goodsDTO.getCategoryId());

        Goods goods = new Goods();
        BeanUtils.copyProperties(goodsDTO, goods);
        // 带状态的数据新增后一律禁用（全局约定），需在列表中手动上架
        goods.setStatus(StatusConstant.DISABLE);

        // 名称重复由 goods 表的唯一索引兜住，GlobalExceptionHandler 会转成「xxx已存在」
        goodsMapper.insert(goods);
        // 此时商品主键已回填，接着写规格
        int specCount = saveSpecs(goods.getId(), goodsDTO.getSpecs());

        log.info("新增商品成功：id={}，name={}，categoryId={}，specs={} 组",
                goods.getId(), goods.getName(), goods.getCategoryId(), specCount);
    }

    @Override
    public PageResult pageQuery(GoodsPageQueryDTO queryDTO) {
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize());
        List<GoodsVO> list = goodsMapper.pageQuery(queryDTO);
        Page<GoodsVO> page = (Page<GoodsVO>) list;
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 删除商品
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            // 空集合直接返回：既不报错也不执行任何 SQL（IN () 是非法语法）
            log.info("批量删除商品：未提交任何 id，忽略");
            return;
        }

        // 上架中不能删：正在售卖的商品突然消失，用户下单会失败
        if (goodsMapper.countOnSaleByIds(ids) > 0) {
            log.warn("商品删除被拒：{} 中存在上架中的商品", ids);
            throw new GoodsOnSaleException();
        }
        // 被组合箱引用不能删：引用不看组合箱状态，停售中的箱子恢复销售时就会缺件
        if (bundleGoodsMapper.countByGoodsIds(ids) > 0) {
            log.warn("商品删除被拒：{} 中存在被组合箱引用的商品", ids);
            throw new GoodsRelatedByBundleException();
        }

        int deleted = goodsMapper.deleteByIds(ids);
        goodsSpecMapper.deleteByGoodsIds(ids);
        // 购物车里指向这些商品的行已无意义，留着会在结算时报「商品不存在」
        cartMapper.deleteByGoodsIds(ids);

        log.info("删除商品成功：请求 {} 个，实际删除 {} 个", ids.size(), deleted);
    }

    @Override
    public GoodsVO getById(Long id) {
        GoodsVO vo = goodsMapper.getVoById(id);
        if (vo == null) {
            throw new GoodsNotFoundException();
        }
        vo.setSpecs(loadSpecVos(id));
        return vo;
    }

    @Override
    @Transactional
    public void update(GoodsDTO goodsDTO) {
        Goods existing = requireGoods(goodsDTO.getId());
        assertCategoryUsable(goodsDTO.getCategoryId());
        // 上架中的商品改到一个已禁用的分类，会造出「上架中但分类已禁用」的矛盾状态
        if (StatusConstant.ENABLE.equals(existing.getStatus())) {
            assertCategoryEnabled(goodsDTO.getCategoryId());
        }

        Goods goods = new Goods();
        BeanUtils.copyProperties(goodsDTO, goods);
        // 动态更新：只写提交的字段；status 不在该语句内，状态只能走状态接口
        goodsMapper.update(goods);

        // 规格全量覆盖：先按商品 id 清空旧的，再插入本次提交的（契约 2.6.5）
        goodsSpecMapper.deleteByGoodsId(goods.getId());
        int specCount = saveSpecs(goods.getId(), goodsDTO.getSpecs());

        // 上架中的商品改了名称/价格/分类，用户端缓存必须跟着失效，否则最长 30 分钟看不到新价。
        // 分类可能被改过，新旧两个键都要清。
        if (StatusConstant.ENABLE.equals(existing.getStatus())) {
            clearCategoryCache(existing.getCategoryId());
            clearCategoryCache(goodsDTO.getCategoryId());
        }

        log.info("修改商品成功：id={}，name={}，specs={} 组", goods.getId(), goods.getName(), specCount);
    }

    @Override
    public List<Goods> listByCategoryId(Long categoryId) {
        return goodsMapper.listByCategoryId(categoryId);
    }

    /**
     * 上架 / 下架
     * @param status
     * @param id
     */
    @Override
    @Transactional
    public void setStatus(Integer status, Long id) {
        Goods goods = requireGoods(id);

        // 上架：所属分类必须启用，否则用户在分类页看不到它、只有搜索能搜到
        if (StatusConstant.ENABLE.equals(status)) {
            assertCategoryEnabled(goods.getCategoryId());
        }
        // 下架：不能被起售中的组合箱引用，否则在售的箱子会缺件
        else if (bundleGoodsMapper.countOnSaleByGoodsIds(List.of(id)) > 0) {
            log.warn("商品下架被拒：id={} 仍被起售中的组合箱引用", id);
            throw new GoodsDisableFailedException();
        }

        Goods update = new Goods();
        update.setId(id);
        update.setStatus(status);
        goodsMapper.updateStatus(update);

        // 精准清除该商品所属分类下的用户端缓存，不清全量
        clearCategoryCache(goods.getCategoryId());

        log.info("商品状态已变更：id={}，status={}，已清除该分类的用户端缓存", id, status);
    }

    //以下为内部方法

    /**
     * 取商品，取不到即抛「商品不存在」
     * @param id
     * @return
     */
    private Goods requireGoods(Long id) {
        // 修改接口漏传 id 时也会走到这里：getById(null) 查不到记录，
        // 返回的同样是「商品不存在」，前端能看懂。
        Goods goods = goodsMapper.getById(id);
        if (goods == null) {
            throw new GoodsNotFoundException();
        }
        return goods;
    }

    /**
     * 校验分类可用于挂商品：必须存在，且是商品分类（type = 1）
     * @param categoryId
     */
    private void assertCategoryUsable(Long categoryId) {
        GoodsCategory category = goodsCategoryMapper.getById(categoryId);
        if (category == null) {
            throw new CategoryNotFoundException();
        }
        if (!CategoryTypeConstant.GOODS.equals(category.getType())) {
            throw new CategoryTypeMismatchException();
        }
    }

    /**
     * 校验分类处于启用状态（上架的前置条件）
     * @param categoryId
     */
    private void assertCategoryEnabled(Long categoryId) {
        GoodsCategory category = goodsCategoryMapper.getById(categoryId);
        if (category == null) {
            throw new CategoryNotFoundException();
        }
        if (StatusConstant.DISABLE.equals(category.getStatus())) {
            log.warn("商品上架被拒：分类 {} 处于禁用状态", categoryId);
            throw new GoodsEnableFailedException();
        }
    }

    /**
     * 写入规格，返回写入的规格组数
     * @param goodsId
     * @param specs
     * @return
     */
    private int saveSpecs(Long goodsId, List<GoodsSpecDTO> specs) {
        // 候选值在接口层是数组，这里序列化成 JSON 文本落库；
        // 空白候选值直接丢弃，一个候选值都不剩的规格组视为非法（前端也拦了同样的规则，
        // 这里是不信任前端的那一道）。
        if (specs == null || specs.isEmpty()) {
            return 0;
        }
        List<GoodsSpec> entities = new ArrayList<>(specs.size());
        for (GoodsSpecDTO specDTO : specs) {
            String name = specDTO.getName() == null ? "" : specDTO.getName().trim();
            List<String> values = specDTO.getValue() == null
                    ? List.of()
                    : specDTO.getValue().stream()
                            .filter(v -> v != null && !v.isBlank())
                            .map(String::trim)
                            .toList();
            if (name.isEmpty() || values.isEmpty()) {
                throw new GoodsSpecIncompleteException();
            }

            GoodsSpec spec = new GoodsSpec();
            spec.setGoodsId(goodsId);
            spec.setName(name);
            spec.setValue(JsonUtil.toStringArrayJson(values));
            entities.add(spec);
        }
        goodsSpecMapper.insertBatch(entities);
        return entities.size();
    }

    /**
     * 清除某分类下的用户端商品列表缓存
     * @param categoryId
     */
    private void clearCategoryCache(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        stringRedisTemplate.delete(RedisKeyConstant.GOODS_BY_CATEGORY_PREFIX + categoryId);
    }

    /**
     * 读取规格并把 JSON 文本还原成数组，供详情接口返回
     * @param goodsId
     * @return
     */
    private List<GoodsSpecVO> loadSpecVos(Long goodsId) {
        return goodsSpecMapper.listByGoodsId(goodsId).stream().map(spec -> {
            GoodsSpecVO vo = new GoodsSpecVO();
            vo.setId(spec.getId());
            vo.setName(spec.getName());
            vo.setValue(JsonUtil.toStringArray(spec.getValue()));
            return vo;
        }).toList();
    }
}
