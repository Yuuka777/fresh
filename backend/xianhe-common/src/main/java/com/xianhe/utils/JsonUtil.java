package com.xianhe.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

/**
 * JSON 文本与对象之间的转换工具
 */
public class JsonUtil {

    /**
     * 复用同一个 ObjectMapper
     * @return
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 把字符串数组序列化成 JSON 文本
     * @param values
     * @return
     */
    public static String toStringArrayJson(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(values);
        } catch (Exception ex) {
            // 序列化字符串数组不会失败，真出现问题说明代码有误，直接暴露而不是吞掉
            throw new IllegalStateException("规格候选值序列化失败", ex);
        }
    }

    /**
     * 把 JSON 文本还原成字符串数组
     * @param json
     * @return
     */
    public static List<String> toStringArray(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            /*
              这里刻意不抛异常：数据库里若存在人手改坏的文本，
              详情接口应该照常返回其它规格，而不是整个查询失败。
              该条规格的候选值会被当成空数组，问题在界面上即可看见。
            */
            return Collections.emptyList();
        }
    }

    /**
     * 把任意对象序列化成 JSON 文本，WebSocket 推送的消息体用它
     * @param value
     * @return
     */
    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception ex) {
            // 序列化一个普通 Map 不会失败；真出问题说明代码有误，直接暴露
            throw new IllegalStateException("JSON 序列化失败", ex);
        }
    }

    private JsonUtil() {
    }
}
