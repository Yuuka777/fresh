package com.xianhe.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xianhe.json.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 「按分类缓存的列表」缓存器（用户端的商品列表、组合箱列表共用）
 */
@Component
@Slf4j
public class CategoryListCache {

    // 正常缓存的基础有效期
    private static final long BASE_TTL_MINUTES = 30;

    // 有效期随机抖动上限（分钟）：防止同一时刻写入的一批键同时过期，把流量全压到库上
    private static final int JITTER_MINUTES = 5;

    // 空值哨兵的有效期：短一些，让「新上架的商品」最迟一分钟就能自己出现
    private static final Duration EMPTY_TTL = Duration.ofMinutes(1);

    // 抢锁最长等待、锁自动释放时间
    private static final long LOCK_WAIT_SECONDS = 2;
    private static final long LOCK_LEASE_SECONDS = 5;

    // 空值哨兵的内容：空数组。用 JSON 而不是空串，读的时候不必区分「哨兵」和「脏数据」
    private static final String EMPTY_SENTINEL = "[]";

    // 用全局 JSON 配置：与接口返回的时间格式保持一致，缓存读写不会引入第二套格式
    private static final ObjectMapper MAPPER = new JacksonObjectMapper();

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 读缓存，未命中则回源并写入
     * @param keyPrefix
     * @param categoryId
     * @param typeRef
     * @param loader
     * @return
     */
    public <T> List<T> getOrLoad(String keyPrefix, Long categoryId, TypeReference<List<T>> typeRef,
                                 Supplier<List<T>> loader) {
        String key = keyPrefix + categoryId;

        List<T> cached = read(key, typeRef);
        if (cached != null) {
            return cached;
        }

        // 未命中：加互斥锁，只让一个线程回源，避免热点分类被并发打穿
        RLock lock = redissonClient.getLock(key + ":lock");
        boolean locked = false;
        try {
            locked = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                // 没抢到说明别人正在回源：再读一次，仍为空就先返回空列表，不把用户挂住
                log.info("{} 正在回源，本次不阻塞", key);
                List<T> again = read(key, typeRef);
                return again == null ? List.of() : again;
            }

            // 等到锁之后可能已经被别人写好了，再确认一次
            List<T> afterLock = read(key, typeRef);
            if (afterLock != null) {
                return afterLock;
            }

            List<T> loaded = loader.get();
            write(key, loaded, typeRef);
            log.info("{} 回源完成，{} 条", key, loaded.size());
            return loaded;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            // 被中断就别再等了，直接查库，保证用户拿得到数据
            return loader.get();
        } catch (Exception ex) {
            // Redis 出问题不应让查询失败：退化成直查数据库
            log.warn("缓存读写异常，改为直查数据库：{}", ex.getMessage());
            return loader.get();
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // 命中返回值列表；未命中返回 null（注意「命中空值哨兵」返回的是空列表，不是 null）
    private <T> List<T> read(String key, TypeReference<List<T>> typeRef) {
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        if (json.isBlank() || EMPTY_SENTINEL.equals(json)) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (Exception ex) {
            // 值坏了就当作未命中，回源重建
            log.warn("缓存内容解析失败，按未命中处理：{}", ex.getMessage());
            return null;
        }
    }

    private <T> void write(String key, List<T> list, TypeReference<List<T>> typeRef) {
        try {
            if (list == null || list.isEmpty()) {
                // 空值哨兵：库里确实没有，也缓存起来，防止同一个空分类被反复穿透
                stringRedisTemplate.opsForValue().set(key, EMPTY_SENTINEL, EMPTY_TTL);
                return;
            }
            long ttlMinutes = BASE_TTL_MINUTES + ThreadLocalRandom.current().nextInt(JITTER_MINUTES + 1);
            stringRedisTemplate.opsForValue()
                    .set(key, MAPPER.writeValueAsString(list), Duration.ofMinutes(ttlMinutes));
        } catch (Exception ex) {
            // 写缓存失败不影响本次查询
            log.warn("写缓存失败：{}", ex.getMessage());
        }
    }
}
