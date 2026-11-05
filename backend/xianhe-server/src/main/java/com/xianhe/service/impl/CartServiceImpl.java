package com.xianhe.service.impl;

import com.xianhe.constant.StatusConstant;
import com.xianhe.context.BaseContext;
import com.xianhe.dto.CartDTO;
import com.xianhe.entity.Bundle;
import com.xianhe.entity.Cart;
import com.xianhe.entity.Goods;
import com.xianhe.entity.OrderItem;
import com.xianhe.exception.BundleNotFoundException;
import com.xianhe.exception.CartEmptyException;
import com.xianhe.exception.CartTargetRequiredException;
import com.xianhe.exception.GoodsNotFoundException;
import com.xianhe.mapper.BundleMapper;
import com.xianhe.mapper.CartMapper;
import com.xianhe.mapper.GoodsMapper;
import com.xianhe.service.CartService;
import com.xianhe.vo.CartVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购物车业务实现
 */
@Service
@Slf4j
public class CartServiceImpl implements CartService {

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private BundleMapper bundleMapper;

    @Override
    @Transactional
    public void add(CartDTO cartDTO) {
        Long memberId = BaseContext.getCurrentId();
        assertTarget(cartDTO);
        normalizeSpecText(cartDTO);

        increase(memberId, cartDTO.getGoodsId(), cartDTO.getBundleId(), cartDTO.getSpecText(), 1);
    }

    @Override
    @Transactional
    public void addFromOrderItems(List<OrderItem> items) {
        Long memberId = BaseContext.getCurrentId();

        for (OrderItem item : items) {
            addRepeatLine(memberId, item);
        }

        log.info("再来一单：memberId={}，{} 项明细写回购物车", memberId, items.size());
    }

    @Override
    public List<CartVO> list() {
        Long memberId = BaseContext.getCurrentId();
        List<Cart> lines = cartMapper.listByMemberId(memberId);
        if (lines.isEmpty()) {
            return Collections.emptyList();
        }

        // 现值按类型各批量查一次（不逐行查，避免 N+1）
        Map<Long, Goods> goodsMap = loadGoods(lines);
        Map<Long, Bundle> bundleMap = loadBundles(lines);

        return lines.stream().map(line -> toVo(line, goodsMap, bundleMap)).collect(Collectors.toList());
    }

    @Override
    public void clean() {
        Long memberId = BaseContext.getCurrentId();
        cartMapper.deleteByMemberId(memberId);
        log.info("购物车已清空：memberId={}", memberId);
    }

    @Override
    @Transactional
    public void sub(CartDTO cartDTO) {
        Long memberId = BaseContext.getCurrentId();
        assertTarget(cartDTO);
        normalizeSpecText(cartDTO);

        Cart same = sameLine(cartDTO, memberId);
        if (same == null) {
            // 契约 7.4：该记录已不在购物车（多为另一台设备刚清空）
            throw new CartEmptyException();
        }

        if (same.getNumber() <= 1) {
            // 减到 0 就删掉这一行，不在购物车里留 number = 0 的残行
            cartMapper.deleteById(same.getId());
            log.info("购物车减到 0，删除该行：memberId={}，cartId={}", memberId, same.getId());
            return;
        }

        BigDecimal unit = unitPrice(same);
        same.setNumber(same.getNumber() - 1);
        same.setAmount(unit.multiply(BigDecimal.valueOf(same.getNumber())));
        cartMapper.updateNumberById(same);

        log.info("购物车减一件：memberId={}，cartId={}，number={}", memberId, same.getId(), same.getNumber());
    }

    @Override
    public void remove(Long id) {
        Long memberId = BaseContext.getCurrentId();

        // 带 member_id 条件，别人的行删不到；删不到就按「车里没有这一行」处理（契约 7.5）
        if (id == null || cartMapper.deleteByIdAndMember(id, memberId) == 0) {
            log.warn("购物车移除失败：该行不在车里，memberId={}，cartId={}", memberId, id);
            throw new CartEmptyException();
        }

        log.info("购物车移除一行：memberId={}，cartId={}", memberId, id);
    }

    // 按购物车里的商品 id 批量查现值
    private Map<Long, Goods> loadGoods(List<Cart> lines) {
        List<Long> ids = lines.stream()
                .map(Cart::getGoodsId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 空集合判定放在这里：Mapper XML 里调用集合方法在 JDK 17 上会炸
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return goodsMapper.listByIds(ids).stream()
                .collect(Collectors.toMap(Goods::getId, Function.identity()));
    }

    // 按购物车里的组合箱 id 批量查现值
    private Map<Long, Bundle> loadBundles(List<Cart> lines) {
        List<Long> ids = lines.stream()
                .map(Cart::getBundleId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return bundleMapper.listByIds(ids).stream()
                .collect(Collectors.toMap(Bundle::getId, Function.identity()));
    }

    // 一行购物车 → 返回给前端的形态：名称、图片、小计用现值，另带可结算状态与变价标记
    private CartVO toVo(Cart line, Map<Long, Goods> goodsMap, Map<Long, Bundle> bundleMap) {
        CartVO vo = new CartVO();
        BeanUtils.copyProperties(line, vo);

        String currentName = null;
        String currentImage = null;
        Integer currentStatus = null;
        BigDecimal currentPrice = null;

        if (line.getGoodsId() != null) {
            Goods goods = goodsMap.get(line.getGoodsId());
            if (goods != null) {
                currentName = goods.getName();
                currentImage = goods.getImage();
                currentStatus = goods.getStatus();
                currentPrice = goods.getPrice();
            }
        } else {
            Bundle bundle = bundleMap.get(line.getBundleId());
            if (bundle != null) {
                currentName = bundle.getName();
                currentImage = bundle.getImage();
                currentStatus = bundle.getStatus();
                currentPrice = bundle.getPrice();
            }
        }

        if (currentPrice == null) {
            // 目标已经查不到：标成不可结算，让用户自己移除。
            // 正常不该出现 —— 删除商品/组合箱时会连带清掉购物车里的引用行，这里兜的是直接改库的残留。
            vo.setStatus(StatusConstant.DISABLE);
            vo.setPriceChanged(false);
            return vo;
        }

        vo.setName(currentName);
        vo.setImage(currentImage);
        vo.setStatus(currentStatus);
        vo.setAmount(currentPrice.multiply(BigDecimal.valueOf(line.getNumber())));
        vo.setPriceChanged(currentPrice.compareTo(unitPrice(line)) != 0);
        return vo;
    }

    // 加购必须二选一：既没给商品也没给组合箱时，后面没法判断加的是什么
    private void assertTarget(CartDTO cartDTO) {
        if (cartDTO.getGoodsId() == null && cartDTO.getBundleId() == null) {
            throw new CartTargetRequiredException();
        }
    }

    // 规格文本的空串等同于「没有规格」：无规格行在库里存的是 NULL，
    // 不归一化就会新增一条 spec_text = '' 的行 —— 它与既有行既不合并，
    // 又因为判重按 NULL 走而永远匹配不到，等于每次加减都在造幽灵行
    private void normalizeSpecText(CartDTO cartDTO) {
        cartDTO.setSpecText(normalizeSpecText(cartDTO.getSpecText()));
    }

    private String normalizeSpecText(String specText) {
        return specText == null || specText.trim().isEmpty() ? null : specText;
    }

    private Cart sameLine(CartDTO cartDTO, Long memberId) {
        return cartMapper.getBySameLine(memberId, cartDTO.getGoodsId(), cartDTO.getBundleId(),
                cartDTO.getSpecText());
    }

    // 快照单价 = 小计 ÷ 数量
    private BigDecimal unitPrice(Cart cart) {
        return cart.getAmount().divide(BigDecimal.valueOf(cart.getNumber()), 2, RoundingMode.HALF_UP);
    }

    // 按加购对象回查并写入快照
    private void snapshot(Cart cart) {
        if (cart.getGoodsId() != null) {
            Goods goods = goodsMapper.getById(cart.getGoodsId());
            if (goods == null) {
                throw new GoodsNotFoundException();
            }
            cart.setName(goods.getName());
            cart.setImage(goods.getImage());
            cart.setAmount(goods.getPrice());
            return;
        }

        Bundle bundle = bundleMapper.getById(cart.getBundleId());
        if (bundle == null) {
            throw new BundleNotFoundException();
        }
        cart.setName(bundle.getName());
        cart.setImage(bundle.getImage());
        cart.setAmount(bundle.getPrice());
    }

    // 同款累加 count 件；没有则新建一行并带出名称、图片、单价快照
    private void increase(Long memberId, Long goodsId, Long bundleId, String specText, int count) {
        Cart same = cartMapper.getBySameLine(memberId, goodsId, bundleId, specText);
        if (same != null) {
            // 同款已存在：数量累加，小计按这一行自己的快照单价重算（不悄悄改用当前售价）
            BigDecimal unit = unitPrice(same);
            same.setNumber(same.getNumber() + count);
            same.setAmount(unit.multiply(BigDecimal.valueOf(same.getNumber())));
            cartMapper.updateNumberById(same);

            log.info("购物车同款累加：memberId={}，cartId={}，+{}，number={}",
                    memberId, same.getId(), count, same.getNumber());
            return;
        }

        Cart cart = new Cart();
        cart.setMemberId(memberId);
        cart.setGoodsId(goodsId);
        cart.setBundleId(bundleId);
        cart.setSpecText(specText);
        cart.setNumber(count);
        cart.setCreateTime(LocalDateTime.now());
        // 新建这一行时带出名称、图片、单价快照；snapshot 写的是单价，小计按数量展开
        snapshot(cart);
        cart.setAmount(cart.getAmount().multiply(BigDecimal.valueOf(count)));

        cartMapper.insert(cart);
        log.info("购物车新增一行：memberId={}，cartId={}，name={}，number={}",
                memberId, cart.getId(), cart.getName(), count);
    }

    // 复购写回一行：同款交给 increase 累加；没有同款时先按订单里的快照建行，再尽量刷成现值
    private void addRepeatLine(Long memberId, OrderItem item) {
        String specText = normalizeSpecText(item.getSpecText());

        if (cartMapper.getBySameLine(memberId, item.getGoodsId(), item.getBundleId(), specText) != null) {
            increase(memberId, item.getGoodsId(), item.getBundleId(), specText, item.getNumber());
            return;
        }

        Cart cart = new Cart();
        cart.setMemberId(memberId);
        cart.setGoodsId(item.getGoodsId());
        cart.setBundleId(item.getBundleId());
        cart.setSpecText(specText);
        cart.setNumber(item.getNumber());
        cart.setCreateTime(LocalDateTime.now());
        // 订单里的快照先顶上；商品已被删除时就以它落库 ——
        // 购物车列表会把查不到现值的行标成不可结算，用户看得见并自行移除，比静默少一件更清楚
        cart.setName(item.getName());
        cart.setImage(item.getImage());
        cart.setAmount(item.getAmount());
        refreshSnapshot(cart);

        cartMapper.insert(cart);
    }

    // 能查到现值就刷新名称、图片与小计；查不到就保留调用方已填的快照（复购专用，不抛异常）
    private void refreshSnapshot(Cart cart) {
        BigDecimal unit;
        if (cart.getGoodsId() != null) {
            Goods goods = goodsMapper.getById(cart.getGoodsId());
            if (goods == null) {
                return;
            }
            cart.setName(goods.getName());
            cart.setImage(goods.getImage());
            unit = goods.getPrice();
        } else {
            Bundle bundle = bundleMapper.getById(cart.getBundleId());
            if (bundle == null) {
                return;
            }
            cart.setName(bundle.getName());
            cart.setImage(bundle.getImage());
            unit = bundle.getPrice();
        }
        cart.setAmount(unit.multiply(BigDecimal.valueOf(cart.getNumber())));
    }
}
