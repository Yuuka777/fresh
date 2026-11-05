package com.xianhe.service.impl;

import com.xianhe.constant.RedisKeyConstant;
import com.xianhe.constant.StatusConstant;
import com.xianhe.service.ShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 店铺业务实现
 */
@Service
@Slf4j
public class ShopServiceImpl implements ShopService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void setStatus(Integer status) {
        stringRedisTemplate.opsForValue()
                .set(RedisKeyConstant.SHOP_STATUS, String.valueOf(status));
        log.info("门店营业状态已设置：{}", status);
    }

    @Override
    public Integer getStatus() {
        String value = stringRedisTemplate.opsForValue().get(RedisKeyConstant.SHOP_STATUS);
        if (value == null) {
            // Redis 是易失存储，重启后可能没有这个 key。
            // 默认按「营业中」返回，而不是把 null 抛给前端：
            // 前端用它决定能否下单，需要一个确定的布尔语义。
            return StatusConstant.ENABLE;
        }
        return Integer.valueOf(value);
    }
}
