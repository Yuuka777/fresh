package com.xianhe.config;

import com.xianhe.properties.JwtProperties;
import com.xianhe.websocket.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import javax.annotation.PostConstruct;

/**
 * WebSocket 配置：把 @ServerEndpoint 标注的端点注册进容器
 */
@Configuration
public class WebSocketConfiguration {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 端点的实例生命周期由 WebSocket 容器掌握，直接往端点类里 @Autowired 不生效（实测令牌校验恒失败）。
     * 这里在启动时把 JWT 配置交给端点的静态字段，保证握手验签拿得到密钥。
     */
    @PostConstruct
    public void initEndpointDependencies() {
        WebSocketServer.setJwtProperties(jwtProperties);
    }

    /**
     * @return
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
