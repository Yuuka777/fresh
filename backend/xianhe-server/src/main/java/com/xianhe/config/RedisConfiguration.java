package com.xianhe.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Redis 相关配置
 */
@Configuration
@Slf4j
public class RedisConfiguration {

    @Autowired
    private RedisProperties redisProperties;

    /**
     * Redisson 客户端，用于分布式锁与限流
     * @return
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        // destroyMethod = "shutdown" 让容器关闭时释放 Netty 线程池，
        // 否则单元测试与热部署会残留线程。
        Config config = new Config();
        SingleServerConfig server = config.useSingleServer()
                .setAddress("redis://" + redisProperties.getHost() + ":" + redisProperties.getPort())
                .setDatabase(redisProperties.getDatabase());
        if (StringUtils.hasText(redisProperties.getPassword())) {
            server.setPassword(redisProperties.getPassword());
        }

        log.info("创建 Redisson 客户端：{}:{}/db{}",
                redisProperties.getHost(), redisProperties.getPort(), redisProperties.getDatabase());
        return Redisson.create(config);
    }
}
