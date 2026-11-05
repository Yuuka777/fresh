package com.xianhe.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.xianhe.exception.FileUploadFailedException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;

/**
 * 阿里云 OSS 工具类
 */
@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    /**
     * 上传文件
     * @param bytes
     * @param objectName
     * @return
     */
    public String upload(byte[] bytes, String objectName) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));
        } catch (OSSException oe) {
            // 请求已送达 OSS，但被拒绝（权限、配额、对象名非法等）
            log.error("上传 OSS 被拒绝，objectName={}，errorCode={}，errorMessage={}，requestId={}",
                    objectName, oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId());
            throw new FileUploadFailedException();
        } catch (ClientException ce) {
            // 本地客户端问题：网络不通、endpoint 配置错误等
            log.error("上传 OSS 客户端异常，objectName={}", objectName, ce);
            throw new FileUploadFailedException();
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        // 访问地址规则：https://{bucketName}.{endpoint}/{objectName}
        String url = "https://" + bucketName + "." + endpoint + "/" + objectName;
        log.info("文件上传成功：{}", url);
        return url;
    }
}
