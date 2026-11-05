package com.xianhe;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 鲜禾前置仓后端启动类
 */
@SpringBootApplication
// 声明式事务：业务方法上标 @Transactional 即可
@EnableTransactionManagement
// 定时任务：订单超时未支付自动取消（契约 2.8.9）
@EnableScheduling
// 缓存注解
@EnableCaching
// 持久层接口统一在本包下，避免每个接口都写 @Mapper
@MapperScan("com.xianhe.mapper")
@Slf4j
public class XianheApplication {

    public static void main(String[] args) {
        SpringApplication.run(XianheApplication.class, args);
        log.info("鲜禾后端启动完成");
    }
}
