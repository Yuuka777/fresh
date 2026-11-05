package com.xianhe.utils;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 轻量 HTTP 客户端工具
 */
@Slf4j
public class HttpClientUtil {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private HttpClientUtil() {
    }

    /**
     * 发送 GET 请求，返回响应体字符串
     * @param url
     * @return
     */
    public static String doGet(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        try {
            HttpResponse<String> response = CLIENT.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new RuntimeException("外部接口返回非 200 状态码：" + response.statusCode());
            }
            return response.body();
        } catch (InterruptedException e) {
            // 恢复中断标记，避免吞掉线程中断信号
            Thread.currentThread().interrupt();
            throw new RuntimeException("调用外部接口被中断", e);
        } catch (Exception e) {
            log.error("调用外部接口失败：{}", url, e);
            throw new RuntimeException("调用外部接口失败", e);
        }
    }

    /**
     * 对查询参数做 URL 编码，避免 code 中的特殊字符破坏请求串
     * @param value
     * @return
     */
    public static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
