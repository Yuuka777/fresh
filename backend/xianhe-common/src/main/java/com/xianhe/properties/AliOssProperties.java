package com.xianhe.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 对象存储（阿里云 OSS）配置
 */
@Component
@ConfigurationProperties(prefix = "xianhe.alioss")
@Data
public class AliOssProperties {

    //地域节点，形如 oss-cn-beijing.aliyuncs.com
    private String endpoint;

    //访问密钥 id
    private String accessKeyId;

    //访问密钥 secret
    private String accessKeySecret;

    //存储空间名称
    private String bucketName;
}
