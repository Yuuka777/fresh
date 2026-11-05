package com.xianhe.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置
 */
@Component
@ConfigurationProperties(prefix = "xianhe.wechat")
@Data
public class WeChatProperties {

    //小程序 AppID
    private String appid;

    //小程序密钥
    private String secret;

    //code 换 openid 的接口地址（放配置里便于将来指向联调环境）
    private String jscode2sessionUrl;
}
